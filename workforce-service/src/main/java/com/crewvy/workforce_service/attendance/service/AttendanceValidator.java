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
            String breakEnd = breakRule.getFixedBreakEnd(); // "14:00"
            String[] parts = breakEnd.split(":");
            maxClockInTime = attendance.getAttendanceDate()
                    .atTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        // AUTO/MANUAL 모드: 정규 출근 + 4시간 (오전 근무 종료 추정)
        else if (workTimeRule != null && workTimeRule.getWorkStartTime() != null) {
            String workStart = workTimeRule.getWorkStartTime(); // "09:00"
            String[] parts = workStart.split(":");
            maxClockInTime = attendance.getAttendanceDate()
                    .atTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]))
                    .plusHours(4); // 09:00 + 4시간 = 13:00 (점심 시작 추정)
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
            String breakStart = breakRule.getFixedBreakStart(); // "13:00"
            String[] parts = breakStart.split(":");
            minClockOutTime = attendance.getAttendanceDate()
                    .atTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        // AUTO/MANUAL 모드: 정규 출근 + 4시간 (오전 근무 종료 추정)
        else if (workTimeRule != null && workTimeRule.getWorkStartTime() != null) {
            String workStart = workTimeRule.getWorkStartTime(); // "09:00"
            String[] parts = workStart.split(":");
            minClockOutTime = attendance.getAttendanceDate()
                    .atTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]))
                    .plusHours(4); // 09:00 + 4시간 = 13:00
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
     */
    public void checkLateness(DailyAttendance dailyAttendance, Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();
        LatenessRuleDto latenessRule = policy.getRuleDetails().getLatenessRule();

        if (workTimeRule == null || workTimeRule.getWorkStartTime() == null) {
            return;
        }

        Integer graceMinutes = (latenessRule != null) ? latenessRule.getLatenessGraceMinutes() : null;
        dailyAttendance.checkAndSetLateness(workTimeRule.getWorkStartTime(), graceMinutes);
    }

    /**
     * 조퇴 여부 체크 및 DailyAttendance 업데이트
     */
    public void checkEarlyLeave(DailyAttendance dailyAttendance, Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();
        LatenessRuleDto latenessRule = policy.getRuleDetails().getLatenessRule();

        if (workTimeRule == null || workTimeRule.getWorkEndTime() == null) {
            return;
        }

        Integer graceMinutes = (latenessRule != null) ? latenessRule.getEarlyLeaveGraceMinutes() : null;
        dailyAttendance.checkAndSetEarlyLeave(workTimeRule.getWorkEndTime(), graceMinutes);
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
                validateFixedPlusHours(workDate, workTimeRule, clockOutRule, clockInTime, clockOutTime);
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
                                       LocalDateTime clockInTime, LocalDateTime clockOutTime) {
        if (workTimeRule == null || workTimeRule.getWorkEndTime() == null) {
            return;
        }

        String[] timeParts = workTimeRule.getWorkEndTime().split(":");
        LocalDateTime standardEndTime = workDate.atTime(
                Integer.parseInt(timeParts[0]),
                Integer.parseInt(timeParts[1])
        );

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

        Integer workedMinutes = dailyAttendance.getWorkedMinutes() +
                (dailyAttendance.getTotalBreakMinutes() != null ? dailyAttendance.getTotalBreakMinutes() : 0);

        Integer mandatoryBreakMinutes = null;

        // 정책에 법정 최소 휴게시간이 설정되어 있으면 사용
        if (policy != null && policy.getRuleDetails() != null) {
            BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();
            if (breakRule != null && breakRule.getMandatoryBreakMinutes() != null) {
                mandatoryBreakMinutes = breakRule.getMandatoryBreakMinutes();
            }
        }

        // 정책에 없으면 근로기준법 기본값
        if (mandatoryBreakMinutes == null) {
            if (workedMinutes >= 480) { // 8시간 이상
                mandatoryBreakMinutes = 60; // 1시간
            } else if (workedMinutes >= 240) { // 4시간 이상
                mandatoryBreakMinutes = 30; // 30분
            } else {
                return; // 4시간 미만은 의무 휴게시간 없음
            }
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
}
