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
     * AUTO 모드일 경우 휴게 시간 자동 계산
     */
    public void autoCalculateBreakTime(DailyAttendance dailyAttendance, Policy policy, LocalDateTime clockOutTime) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();
        if (breakRule == null || !"AUTO".equals(breakRule.getType())) {
            return; // AUTO 모드가 아니면 자동 계산 안 함
        }

        // 출근 시각이 없으면 계산 불가
        if (dailyAttendance.getFirstClockIn() == null) {
            return;
        }

        // 총 근무 시간(휴게 제외 전) 계산
        long totalMinutes = Duration.between(dailyAttendance.getFirstClockIn(), clockOutTime).toMinutes();

        int autoBreakMinutes = 0;

        // 8시간 이상 근무 시 정책의 기본 휴게시간
        if (totalMinutes >= 480 && breakRule.getDefaultBreakMinutesFor8Hours() != null) {
            autoBreakMinutes = breakRule.getDefaultBreakMinutesFor8Hours();
        }
        // 4시간 이상 근무 시 법정 최소 휴게시간
        else if (totalMinutes >= 240 && breakRule.getMandatoryBreakMinutes() != null) {
            autoBreakMinutes = breakRule.getMandatoryBreakMinutes();
        }

        // 휴게시간이 이미 수동으로 기록되어 있으면 덮어쓰지 않음
        if (dailyAttendance.getTotalBreakMinutes() == null || dailyAttendance.getTotalBreakMinutes() == 0) {
            dailyAttendance.setTotalBreakMinutes(autoBreakMinutes);
            log.info("AUTO 모드 휴게시간 자동 계산: memberId={}, date={}, breakMinutes={}",
                    dailyAttendance.getMemberId(), dailyAttendance.getAttendanceDate(), autoBreakMinutes);
        }
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
