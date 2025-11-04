package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.workforce_service.attendance.dto.rule.BreakRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.ClockOutRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.GoOutRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.LatenessRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.WorkTimeRuleDto;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.Policy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 근태 정책 기반 검증 서비스
 * - 지각/조퇴 검증
 * - 근무시간 제한 검증
 * - 반차 출퇴근 시간 검증
 * - 휴게/외출 정책 검증
 */
@Slf4j
@Service
public class AttendanceValidator {

    private LocalTime safeParseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeString);
        } catch (java.time.format.DateTimeParseException e) {
            log.error("잘못된 시간 형식입니다. 'HH:mm' 형식이 필요합니다. 입력값: {}", timeString, e);
            throw new BusinessException("정책의 시간 형식이 잘못되었습니다: " + timeString);
        }
    }

    /**
     * 오전 반차 출근 시간 검증
     * 점심 종료 시간 + 지각 허용 시간까지 출근 가능
     */
    public void validateHalfDayAMClockIn(DailyAttendance attendance, Policy policy, LocalDateTime clockInTime) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();
        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();
        LatenessRuleDto latenessRule = policy.getRuleDetails().getLatenessRule();

        LocalDateTime maxClockInTime;

        // FIXED 휴게 모드: fixedBreakEnd 사용
        if (breakRule != null && "FIXED".equals(breakRule.getType()) && breakRule.getFixedBreakEnd() != null) {
            LocalTime breakEndTime = safeParseTime(breakRule.getFixedBreakEnd());
            maxClockInTime = attendance.getAttendanceDate().atTime(breakEndTime);
        }
        // AUTO/MANUAL 모드: 정규 출근 + 4시간 (오전 근무 종료 추정)
        else if (workTimeRule != null && workTimeRule.getWorkStartTime() != null) {
            LocalTime workStartTime = safeParseTime(workTimeRule.getWorkStartTime());
            maxClockInTime = attendance.getAttendanceDate().atTime(workStartTime).plusHours(4); // 09:00 + 4시간 = 13:00 (점심 시작 추정)
        }
        // 정책 없으면 13:00 기본값
        else {
            maxClockInTime = attendance.getAttendanceDate().atTime(13, 0);
        }

        // 지각 허용 시간 적용
        Integer latenessGraceMinutes = (latenessRule != null) ? latenessRule.getLatenessGraceMinutes() : 0;
        if (latenessGraceMinutes != null && latenessGraceMinutes > 0) {
            maxClockInTime = maxClockInTime.plusMinutes(latenessGraceMinutes);
        }

        // 출근 시간 검증
        if (clockInTime.isAfter(maxClockInTime)) {
            throw new BusinessException(
                    String.format("오전 반차는 %s까지 출근해야 합니다. (현재: %s)",
                            maxClockInTime.toLocalTime(),
                            clockInTime.toLocalTime()));
        }
    }

    /**
     * 오후 반차 퇴근 시간 검증
     * 점심 시작 시간 이후 퇴근 가능
     */
    public void validateHalfDayPMClockOut(DailyAttendance attendance, Policy policy, LocalDateTime clockOutTime) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();
        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();

        LocalDateTime minClockOutTime;

        // FIXED 휴게 모드: fixedBreakStart 사용
        if (breakRule != null && "FIXED".equals(breakRule.getType()) && breakRule.getFixedBreakStart() != null) {
            LocalTime breakStartTime = safeParseTime(breakRule.getFixedBreakStart());
            minClockOutTime = attendance.getAttendanceDate().atTime(breakStartTime);
        }
        // AUTO/MANUAL 모드: 정규 출근 + 4시간 (오전 근무 종료 추정)
        else if (workTimeRule != null && workTimeRule.getWorkStartTime() != null) {
            LocalTime workStartTime = safeParseTime(workTimeRule.getWorkStartTime());
            minClockOutTime = attendance.getAttendanceDate().atTime(workStartTime).plusHours(4); // 09:00 + 4시간 = 13:00
        }
        // 정책 없으면 13:00 기본값
        else {
            minClockOutTime = attendance.getAttendanceDate().atTime(13, 0);
        }

        // 퇴근 시간 검증
        if (clockOutTime.isBefore(minClockOutTime)) {
            throw new BusinessException(
                    String.format("오후 반차는 %s 이후에 퇴근해야 합니다. (현재: %s)",
                            minClockOutTime.toLocalTime(),
                            clockOutTime.toLocalTime()));
        }
    }

    /**
     * 지각 여부 체크 및 DailyAttendance 업데이트
     * - FIXED/DEEMED: 정규 출근시각 기준 지각 체크
     * - FLEXIBLE: 코어타임 시작 시각 기준 체크
     */
    public void checkLateness(DailyAttendance dailyAttendance, Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();
        LatenessRuleDto latenessRule = policy.getRuleDetails().getLatenessRule();

        if (workTimeRule == null) {
            return;
        }

        // 선택근무제(FLEXIBLE)인 경우 코어타임 검증
        if ("FLEXIBLE".equals(workTimeRule.getType()) && workTimeRule.getCoreTimeStart() != null) {
            validateCoreTimeCompliance(dailyAttendance, workTimeRule, latenessRule);
            return;
        }

        // FIXED/DEEMED: 기존 지각 체크
        if (workTimeRule.getWorkStartTime() == null) {
            return;
        }

        Integer graceMinutes = (latenessRule != null) ? latenessRule.getLatenessGraceMinutes() : null;
        dailyAttendance.checkAndSetLateness(workTimeRule.getWorkStartTime(), graceMinutes);
    }

    /**
     * 조퇴 여부 체크 및 DailyAttendance 업데이트
     * - FIXED/DEEMED: 정규 퇴근시각 기준 조퇴 체크
     * - FLEXIBLE: 코어타임은 출근시에만 체크하므로 조퇴 검증 안 함
     */
    public void checkEarlyLeave(DailyAttendance dailyAttendance, Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();
        LatenessRuleDto latenessRule = policy.getRuleDetails().getLatenessRule();

        if (workTimeRule == null) {
            return;
        }

        // 선택근무제(FLEXIBLE)인 경우 조퇴 체크 안 함 (코어타임만 준수하면 됨)
        if ("FLEXIBLE".equals(workTimeRule.getType())) {
            return;
        }

        // FIXED/DEEMED: 기존 조퇴 체크
        if (workTimeRule.getWorkEndTime() == null) {
            return;
        }

        Integer graceMinutes = (latenessRule != null) ? latenessRule.getEarlyLeaveGraceMinutes() : null;
        dailyAttendance.checkAndSetEarlyLeave(workTimeRule.getWorkEndTime(), graceMinutes);
    }

    /**
     * 출근 시각 범위 검증 (정책의 workStartTime ~ workEndTime 내에서만 출근 가능)
     */
    public void validateClockInTimeRange(LocalDateTime clockInTime, Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();
        if (workTimeRule == null || workTimeRule.getWorkStartTime() == null || workTimeRule.getWorkEndTime() == null) {
            return;
        }

        LocalTime time = clockInTime.toLocalTime();

        // 정책의 출퇴근 가능 시간 범위
        LocalTime workStart = safeParseTime(workTimeRule.getWorkStartTime());
        LocalTime workEnd = safeParseTime(workTimeRule.getWorkEndTime());

        // workEnd가 자정을 넘어가는 경우 (예: 18:00 ~ 02:00)
        if (workEnd.isBefore(workStart)) {
            // 자정 이전 또는 자정 이후 허용
            if (time.isBefore(workStart) && time.isAfter(workEnd)) {
                throw new BusinessException(
                        String.format("출근 가능 시간이 아닙니다. (허용 시간: %s ~ 익일 %s)",
                                workTimeRule.getWorkStartTime(), workTimeRule.getWorkEndTime())
                );
            }
        } else {
            // 정상적인 시간 범위 (예: 07:00 ~ 19:00)
            if (time.isBefore(workStart) || time.isAfter(workEnd)) {
                throw new BusinessException(
                        String.format("출근 가능 시간이 아닙니다. (허용 시간: %s ~ %s)",
                                workTimeRule.getWorkStartTime(), workTimeRule.getWorkEndTime())
                );
            }
        }
    }

    /**
     * 퇴근 시각 범위 검증 (정책의 workStartTime ~ workEndTime 내에서만 퇴근 가능)
     */
    public void validateClockOutTimeRange(LocalDateTime clockOutTime, Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();
        if (workTimeRule == null || workTimeRule.getWorkStartTime() == null || workTimeRule.getWorkEndTime() == null) {
            return;
        }

        LocalTime time = clockOutTime.toLocalTime();

        // 정책의 출퇴근 가능 시간 범위
        LocalTime workStart = safeParseTime(workTimeRule.getWorkStartTime());
        LocalTime workEnd = safeParseTime(workTimeRule.getWorkEndTime());

        // workEnd가 자정을 넘어가는 경우
        if (workEnd.isBefore(workStart)) {
            if (time.isBefore(workStart) && time.isAfter(workEnd)) {
                throw new BusinessException(
                        String.format("퇴근 가능 시간이 아닙니다. (허용 시간: %s ~ 익일 %s)",
                                workTimeRule.getWorkStartTime(), workTimeRule.getWorkEndTime())
                );
            }
        } else {
            // 정상적인 시간 범위
            if (time.isBefore(workStart) || time.isAfter(workEnd)) {
                throw new BusinessException(
                        String.format("퇴근 가능 시간이 아닙니다. (허용 시간: %s ~ %s)",
                                workTimeRule.getWorkStartTime(), workTimeRule.getWorkEndTime())
                );
            }
        }
    }

    /**
     * 근무 시간 제한 검증 (출근/퇴근 가능 시간대 확인)
     */
    public void validateWorkingHoursLimit(LocalDate workDate, Policy policy,
                                         LocalDateTime clockInTime, LocalDateTime clockOutTime) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        ClockOutRuleDto clockOutRule = policy.getRuleDetails().getClockOutRule();
        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();

        if (clockOutRule == null || clockOutRule.getLimitType() == null) {
            return;
        }

        String limitType = clockOutRule.getLimitType();

        switch (limitType) {
            case "FIXED_PLUS_HOURS":
                validateFixedPlusHours(workDate, workTimeRule, clockOutRule, clockOutTime);
                break;
            case "END_OF_DAY":
                validateEndOfDay(workDate, clockInTime, clockOutTime);
                break;
            case "WORK_DURATION":
                validateWorkDuration(clockInTime, clockOutTime, clockOutRule);
                break;
            default:
                log.warn("알 수 없는 근무 시간 제한 타입: {}", limitType);
        }
    }

    private void validateFixedPlusHours(LocalDate workDate, WorkTimeRuleDto workTimeRule,
                                       ClockOutRuleDto clockOutRule,
                                       LocalDateTime clockOutTime) {
        if (workTimeRule == null || workTimeRule.getWorkEndTime() == null) {
            return;
        }

        LocalDateTime standardEndTime = workDate.atTime(safeParseTime(workTimeRule.getWorkEndTime()));

        Integer maxHours = clockOutRule.getMaxHoursAfterWorkEnd();
        if (maxHours != null) {
            LocalDateTime maxAllowedTime = standardEndTime.plusHours(maxHours);
            if (clockOutTime != null && clockOutTime.isAfter(maxAllowedTime)) {
                throw new BusinessException("퇴근 시각이 허용 시간을 초과했습니다.");
            }
        }
    }

    private void validateEndOfDay(LocalDate workDate, LocalDateTime clockInTime, LocalDateTime clockOutTime) {
        LocalDateTime endOfDay = workDate.atTime(23, 59, 59);
        if (clockOutTime != null && clockOutTime.isAfter(endOfDay)) {
            throw new BusinessException("당일 자정 이전에 퇴근해야 합니다.");
        }
    }

    private void validateWorkDuration(LocalDateTime clockInTime, LocalDateTime clockOutTime,
                                     ClockOutRuleDto clockOutRule) {
        if (clockInTime == null || clockOutTime == null) {
            return;
        }

        Integer maxHours = clockOutRule.getMaxWorkDurationHours();
        if (maxHours != null) {
            long workedHours = Duration.between(clockInTime, clockOutTime).toHours();
            if (workedHours > maxHours) {
                throw new BusinessException("최대 근무 시간을 초과했습니다.");
            }
        }
    }

    /**
     * 퇴근 시각 검증
     */
    public void validateClockOutTime(DailyAttendance dailyAttendance, Policy policy, LocalDateTime clockOutTime) {
        validateWorkingHoursLimit(
                dailyAttendance.getAttendanceDate(),
                policy,
                dailyAttendance.getFirstClockIn(),
                clockOutTime
        );
    }

    /**
     * 휴게 정책 검증 (일일 최대 휴게 시간)
     */
    public void validateBreakTimePolicy(DailyAttendance dailyAttendance, Policy policy, int additionalMinutes) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();
        if (breakRule == null || breakRule.getMaxDailyBreakMinutes() == null) {
            return;
        }

        int totalBreakMinutes = (dailyAttendance.getTotalBreakMinutes() != null ? dailyAttendance.getTotalBreakMinutes() : 0)
                + additionalMinutes;

        if (totalBreakMinutes > breakRule.getMaxDailyBreakMinutes()) {
            throw new BusinessException(
                    String.format("일일 최대 휴게 시간(%d분)을 초과할 수 없습니다.",
                            breakRule.getMaxDailyBreakMinutes())
            );
        }
    }

    /**
     * 외출 정책 검증 (1회/일일 최대 외출 시간)
     */
    public void validateGoOutTimePolicy(DailyAttendance dailyAttendance, Policy policy, int additionalMinutes) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        GoOutRuleDto goOutRule = policy.getRuleDetails().getGoOutRule();
        if (goOutRule == null) {
            return;
        }

        // 1회 최대 외출 시간 검증
        if (goOutRule.getMaxSingleGoOutMinutes() != null
                && additionalMinutes > goOutRule.getMaxSingleGoOutMinutes()) {
            throw new BusinessException(
                    String.format("1회 최대 외출 시간(%d분)을 초과할 수 없습니다.",
                            goOutRule.getMaxSingleGoOutMinutes())
            );
        }

        // 일일 최대 외출 시간 검증
        if (goOutRule.getMaxDailyGoOutMinutes() != null) {
            int totalGoOutMinutes = (dailyAttendance.getTotalGoOutMinutes() != null ? dailyAttendance.getTotalGoOutMinutes() : 0)
                    + additionalMinutes;

            if (totalGoOutMinutes > goOutRule.getMaxDailyGoOutMinutes()) {
                throw new BusinessException(
                        String.format("일일 최대 외출 시간(%d분)을 초과할 수 없습니다.",
                                goOutRule.getMaxDailyGoOutMinutes())
                );
            }
        }
    }

    /**
     * 휴게 타입이 MANUAL 모드인지 검증
     */
    public void validateBreakIsManualMode(Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            throw new BusinessException("정책 정보를 찾을 수 없습니다.");
        }

        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();
        if (breakRule == null || !"MANUAL".equals(breakRule.getType())) {
            throw new BusinessException("수동 휴게 기록이 허용되지 않는 정책입니다.");
        }
    }

    /**
     * 법정 최소 휴게 시간 검증 (근로기준법 제54조)
     * 4시간 근무 시 30분, 8시간 근무 시 1시간
     */
    public void validateMandatoryBreakTime(DailyAttendance dailyAttendance, Policy policy) {
        if (dailyAttendance.getWorkedMinutes() == null) {
            return;
        }

        // 실제 근무 시간 + 휴게 시간 = 출퇴근 사이 총 시간
        Integer totalMinutesAtWork = dailyAttendance.getWorkedMinutes() +
                (dailyAttendance.getTotalBreakMinutes() != null ? dailyAttendance.getTotalBreakMinutes() : 0);

        Integer mandatoryBreakMinutes = null;
        BreakRuleDto breakRule = null;

        if (policy != null && policy.getRuleDetails() != null) {
            breakRule = policy.getRuleDetails().getBreakRule();
        }

        // 근무 시간에 따라 필수 휴게 시간 결정
        if (totalMinutesAtWork >= 480) { // 8시간 이상
            if (breakRule != null && breakRule.getDefaultBreakMinutesFor8Hours() != null) {
                mandatoryBreakMinutes = breakRule.getDefaultBreakMinutesFor8Hours();
            }
            if (mandatoryBreakMinutes == null) {
                mandatoryBreakMinutes = 60; // 법정 기본값
            }
        } else if (totalMinutesAtWork >= 240) { // 4시간 이상 8시간 미만
            if (breakRule != null && breakRule.getMandatoryBreakMinutes() != null) {
                mandatoryBreakMinutes = breakRule.getMandatoryBreakMinutes();
            }
            if (mandatoryBreakMinutes == null) {
                mandatoryBreakMinutes = 30; // 법정 기본값
            }
        } else {
            return; // 4시간 미만은 의무 휴게시간 없음
        }

        Integer actualBreakMinutes = dailyAttendance.getTotalBreakMinutes() != null
                ? dailyAttendance.getTotalBreakMinutes() : 0;

        if (actualBreakMinutes < mandatoryBreakMinutes) {
            throw new BusinessException(
                    String.format("법정 최소 휴게 시간(%d분)을 충족하지 못했습니다. (현재: %d분)",
                            mandatoryBreakMinutes, actualBreakMinutes)
            );
        }
    }

    /**
     * 선택근무제(FLEXIBLE) 코어타임 준수 여부 검증
     * - 코어타임 시작 전에 출근했는지 확인
     * - 코어타임 종료 후에 퇴근했는지 확인
     */
    private void validateCoreTimeCompliance(DailyAttendance dailyAttendance,
                                            WorkTimeRuleDto workTimeRule,
                                            LatenessRuleDto latenessRule) {
        if (dailyAttendance.getFirstClockIn() == null) {
            return;
        }

        String coreTimeStart = workTimeRule.getCoreTimeStart(); // "10:00"
        String coreTimeEnd = workTimeRule.getCoreTimeEnd();     // "15:00"

        if (coreTimeStart == null || coreTimeEnd == null) {
            log.warn("선택근무제이지만 코어타임이 설정되지 않음");
            return;
        }

        try {
            // 코어타임 시작 시각
            LocalDateTime coreStartDateTime = dailyAttendance.getAttendanceDate().atTime(safeParseTime(coreTimeStart));

            // 지각 허용 시간 적용
            Integer latenessGraceMinutes = (latenessRule != null) ? latenessRule.getLatenessGraceMinutes() : 0;
            if (latenessGraceMinutes != null && latenessGraceMinutes > 0) {
                coreStartDateTime = coreStartDateTime.plusMinutes(latenessGraceMinutes);
            }

            // 코어타임 시작 시각 이후에 출근하면 지각
            if (dailyAttendance.getFirstClockIn().isAfter(coreStartDateTime)) {
                dailyAttendance.setIsLate(true);
                long lateMinutes = Duration.between(
                        coreStartDateTime.minusMinutes(latenessGraceMinutes != null ? latenessGraceMinutes : 0),
                        dailyAttendance.getFirstClockIn()
                ).toMinutes();
                log.info("선택근무제 코어타임 위반(지각): memberId={}, 출근={}, 코어타임 시작={}, 지각시간={}분",
                        dailyAttendance.getMemberId(),
                        dailyAttendance.getFirstClockIn().toLocalTime(),
                        coreTimeStart,
                        lateMinutes);
            }

            // 코어타임 종료 시각 이전에 퇴근하면 조퇴 (퇴근 시각이 있을 경우에만)
            if (dailyAttendance.getLastClockOut() != null && coreTimeEnd != null) {
                LocalDateTime coreEndDateTime = dailyAttendance.getAttendanceDate().atTime(safeParseTime(coreTimeEnd));

                // 조퇴 허용 시간 적용
                Integer earlyLeaveGraceMinutes = (latenessRule != null) ? latenessRule.getEarlyLeaveGraceMinutes() : 0;
                if (earlyLeaveGraceMinutes != null && earlyLeaveGraceMinutes > 0) {
                    coreEndDateTime = coreEndDateTime.minusMinutes(earlyLeaveGraceMinutes);
                }

                if (dailyAttendance.getLastClockOut().isBefore(coreEndDateTime)) {
                    dailyAttendance.setIsEarlyLeave(true);
                    long earlyMinutes = Duration.between(
                            dailyAttendance.getLastClockOut(),
                            coreEndDateTime.plusMinutes(earlyLeaveGraceMinutes != null ? earlyLeaveGraceMinutes : 0)
                    ).toMinutes();
                    log.info("선택근무제 코어타임 위반(조퇴): memberId={}, 퇴근={}, 코어타임 종료={}, 조퇴시간={}분",
                            dailyAttendance.getMemberId(),
                            dailyAttendance.getLastClockOut().toLocalTime(),
                            coreTimeEnd,
                            earlyMinutes);
                }
            }

        } catch (Exception e) {
            log.error("선택근무제 코어타임 검증 실패", e);
        }
    }
}
