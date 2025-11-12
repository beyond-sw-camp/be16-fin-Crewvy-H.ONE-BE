package com.crewvy.workforce_service.attendance.service;

import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import com.crewvy.workforce_service.attendance.dto.rule.BreakRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.WorkTimeRuleDto;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.repository.CompanyHolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 근태 계산 서비스
 * - 근무시간 계산
 * - 휴게시간 자동 계산
 * - 요구 근무시간 계산 (반차/시차 고려)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceCalculator {

    private final CompanyHolidayRepository companyHolidayRepository;

    /**
     * 반차/시차에 따른 요구 근무시간 계산
     * - 정상 근무: 표준 근무시간 (예: 480분 = 8시간)
     * - 반차: 표준 근무시간의 절반 (예: 240분 = 4시간)
     * - 시차: 표준 근무시간 - 시차 신청 시간 (향후 구현)
     */
    public Integer calculateRequiredWorkMinutes(DailyAttendance attendance, Integer standardWorkMinutes) {
        if (standardWorkMinutes == null) {
            standardWorkMinutes = 480; // 기본 8시간
        }

        // 반차인 경우 절반
        if (attendance.getStatus() == AttendanceStatus.HALF_DAY_AM
                || attendance.getStatus() == AttendanceStatus.HALF_DAY_PM) {
            return standardWorkMinutes / 2;
        }

        // TODO: 시차(TIME_OFF) 처리
        // 시차 신청 시간을 조회해서 차감 필요
        // Request 엔티티와 연계 필요

        // 정상 근무
        return standardWorkMinutes;
    }

    /**
     * 정책에서 표준 근무 시간(분) 추출
     */
    public Integer getStandardWorkMinutes(Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return 480; // 기본 8시간
        }

        WorkTimeRuleDto workTimeRule = policy.getRuleDetails().getWorkTimeRule();
        if (workTimeRule == null || workTimeRule.getFixedWorkMinutes() == null) {
            return 480; // 기본 8시간
        }

        return workTimeRule.getFixedWorkMinutes();
    }

    /**
     * 휴게 시간 자동 계산 (AUTO/FIXED 모드)
     * - AUTO: 근무시간에 따라 자동 차감
     * - FIXED: 정책에 설정된 고정 시간 차감 (예: 12:00-13:00)
     * - MANUAL: 수동 기록만 허용 (자동 계산 안 함)
     */
    public void autoCalculateBreakTime(DailyAttendance dailyAttendance, Policy policy, LocalDateTime clockOutTime) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();
        if (breakRule == null || breakRule.getType() == null) {
            return;
        }

        // MANUAL 모드는 자동 계산 안 함
        if ("MANUAL".equals(breakRule.getType())) {
            return;
        }

        // 출근 시각이 없으면 계산 불가
        if (dailyAttendance.getFirstClockIn() == null) {
            return;
        }

        int calculatedBreakMinutes = 0;

        // FIXED 모드: 고정 휴게 시간 차감
        if ("FIXED".equals(breakRule.getType())) {
            calculatedBreakMinutes = calculateFixedBreakMinutes(breakRule, dailyAttendance, clockOutTime);
            if (calculatedBreakMinutes > 0) {
                log.info("FIXED 모드 휴게시간 자동 적용: memberId={}, date={}, breakMinutes={}",
                        dailyAttendance.getMemberId(), dailyAttendance.getAttendanceDate(), calculatedBreakMinutes);
            }
        }
        // AUTO 모드: 근무시간에 따라 자동 계산
        else if ("AUTO".equals(breakRule.getType())) {
            calculatedBreakMinutes = calculateAutoBreakMinutes(breakRule, dailyAttendance, clockOutTime);
            if (calculatedBreakMinutes > 0) {
                log.info("AUTO 모드 휴게시간 자동 계산: memberId={}, date={}, breakMinutes={}",
                        dailyAttendance.getMemberId(), dailyAttendance.getAttendanceDate(), calculatedBreakMinutes);
            }
        }

        // 휴게시간이 이미 수동으로 기록되어 있으면 덮어쓰지 않음
        if (dailyAttendance.getTotalBreakMinutes() == null || dailyAttendance.getTotalBreakMinutes() == 0) {
            dailyAttendance.setTotalBreakMinutes(calculatedBreakMinutes);
        }
    }

    /**
     * FIXED 모드 휴게시간 계산
     * 정책에 설정된 고정 휴게 시간대(예: 12:00-13:00)를 분 단위로 계산
     */
    private int calculateFixedBreakMinutes(BreakRuleDto breakRule, DailyAttendance dailyAttendance, LocalDateTime clockOutTime) {
        if (breakRule.getFixedBreakStart() == null || breakRule.getFixedBreakEnd() == null) {
            log.warn("FIXED 모드이지만 fixedBreakStart 또는 fixedBreakEnd가 설정되지 않음: memberId={}, date={}",
                    dailyAttendance.getMemberId(), dailyAttendance.getAttendanceDate());
            return 0;
        }

        try {
            // "12:00" 형식 파싱
            String[] startParts = breakRule.getFixedBreakStart().split(":");
            String[] endParts = breakRule.getFixedBreakEnd().split(":");

            LocalDateTime breakStart = dailyAttendance.getAttendanceDate()
                    .atTime(Integer.parseInt(startParts[0]), Integer.parseInt(startParts[1]));
            LocalDateTime breakEnd = dailyAttendance.getAttendanceDate()
                    .atTime(Integer.parseInt(endParts[0]), Integer.parseInt(endParts[1]));

            long breakMinutes = Duration.between(breakStart, breakEnd).toMinutes();

            if (breakMinutes <= 0) {
                log.warn("FIXED 휴게시간이 0 이하입니다: memberId={}, date={}, start={}, end={}",
                        dailyAttendance.getMemberId(), dailyAttendance.getAttendanceDate(),
                        breakRule.getFixedBreakStart(), breakRule.getFixedBreakEnd());
                return 0;
            }

            // 실제 근무 시간이 휴게 시간보다 짧으면 휴게시간을 0으로 처리
            if (dailyAttendance.getFirstClockIn() != null && clockOutTime != null) {
                long actualWorkMinutes = Duration.between(
                    dailyAttendance.getFirstClockIn(),
                    clockOutTime
                ).toMinutes();

                // 실제 근무 시간이 휴게시간보다 짧으면 휴게 시간 없음으로 처리
                if (actualWorkMinutes < breakMinutes) {
                    log.info("실제 근무시간({})이 FIXED 휴게시간({})보다 짧아 휴게시간 미적용: memberId={}, date={}",
                            actualWorkMinutes, breakMinutes,
                            dailyAttendance.getMemberId(), dailyAttendance.getAttendanceDate());
                    return 0;
                }
            }

            return (int) breakMinutes;

        } catch (Exception e) {
            log.error("FIXED 휴게시간 계산 실패: memberId={}, date={}, error={}",
                    dailyAttendance.getMemberId(), dailyAttendance.getAttendanceDate(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * AUTO 모드 휴게시간 계산
     * 근무시간에 따라 자동으로 휴게시간 결정
     */
    private int calculateAutoBreakMinutes(BreakRuleDto breakRule, DailyAttendance dailyAttendance, LocalDateTime clockOutTime) {
        // 총 근무 시간(휴게 제외 전) 계산
        long totalMinutes = Duration.between(dailyAttendance.getFirstClockIn(), clockOutTime).toMinutes();

        // 8시간 이상 근무 시 정책의 기본 휴게시간
        if (totalMinutes >= 480 && breakRule.getDefaultBreakMinutesFor8Hours() != null) {
            return breakRule.getDefaultBreakMinutesFor8Hours();
        }
        // 4시간 이상 근무 시 법정 최소 휴게시간
        else if (totalMinutes >= 240 && breakRule.getMandatoryBreakMinutes() != null) {
            return breakRule.getMandatoryBreakMinutes();
        }

        return 0;
    }

    /**
     * 휴일 여부 확인 (주말 또는 회사 휴일)
     */
    public boolean isHoliday(UUID companyId, LocalDate date) {
        // 주말 체크
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true;
        }

        // 회사 휴일 체크
        return companyHolidayRepository.existsByCompanyIdAndHolidayDate(companyId, date);
    }
}
