package com.crewvy.workforce_service.attendance.batch;

import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.Request;
import com.crewvy.workforce_service.attendance.repository.DailyAttendanceRepository;
import com.crewvy.workforce_service.attendance.repository.RequestRepository;
import com.crewvy.workforce_service.attendance.service.AnnualLeaveAccrualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AttendanceBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final RequestRepository requestRepository;
    private final AnnualLeaveAccrualService annualLeaveAccrualService;
    private final com.crewvy.workforce_service.attendance.repository.PolicyRepository policyRepository;

    // ==================== Job 1: 결근 자동 처리 ====================

    @Bean
    public Job markAbsentJob() {
        return new JobBuilder("markAbsentJob", jobRepository)
                .start(markAbsentStep())
                .build();
    }

    @Bean
    public Step markAbsentStep() {
        return new StepBuilder("markAbsentStep", jobRepository)
                .tasklet(markAbsentTasklet(), transactionManager)
                .build();
    }

    /**
     * 결근 자동 처리 Tasklet
     * 전날 출근하지 않은 직원을 ABSENT로 처리
     */
    @Bean
    @Transactional
    public Tasklet markAbsentTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info(">>> [결근 처리 배치 시작] 대상 날짜: {}", yesterday);

            // 1. 전날에 승인된 휴가를 가진 직원 목록 조회
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

            List<UUID> memberIdsWithApprovedLeave = getMemberIdsWithApprovedLeave(startOfDay, endOfDay);
            log.info(">>> 승인된 휴가가 있는 직원 수: {}", memberIdsWithApprovedLeave.size());

            // 2. 전날 DailyAttendance가 이미 있는 직원 목록 조회
            List<DailyAttendance> existingAttendances = dailyAttendanceRepository
                    .findAllByDateRange(yesterday, yesterday);

            Set<UUID> memberIdsWithAttendance = existingAttendances.stream()
                    .map(DailyAttendance::getMemberId)
                    .collect(Collectors.toSet());
            log.info(">>> 이미 근태 기록이 있는 직원 수: {}", memberIdsWithAttendance.size());

            // 3. 최근 30일 내 출근 기록이 있는 활성 직원 조회 (활성 직원 판단 기준)
            LocalDate thirtyDaysAgo = yesterday.minusDays(30);
            List<DailyAttendance> recentAttendances = dailyAttendanceRepository
                    .findAllByDateRange(thirtyDaysAgo, yesterday.minusDays(1));

            Set<UUID> activeMemberIds = recentAttendances.stream()
                    .map(DailyAttendance::getMemberId)
                    .collect(Collectors.toSet());
            log.info(">>> 최근 30일 내 활성 직원 수: {}", activeMemberIds.size());

            // 4. 결근 대상: 활성 직원 중 전날 근태 기록도 없고 승인된 휴가도 없는 직원
            Set<UUID> absentMemberIds = new HashSet<>(activeMemberIds);
            absentMemberIds.removeAll(memberIdsWithAttendance);
            absentMemberIds.removeAll(memberIdsWithApprovedLeave);

            log.info(">>> 결근 처리 대상 직원 수: {}", absentMemberIds.size());

            // 5. 결근 처리 - DailyAttendance 생성
            int createdCount = 0;
            for (UUID memberId : absentMemberIds) {
                // companyId는 해당 직원의 최근 근태 기록에서 가져오기
                UUID companyId = recentAttendances.stream()
                        .filter(da -> da.getMemberId().equals(memberId))
                        .findFirst()
                        .map(DailyAttendance::getCompanyId)
                        .orElse(null);

                if (companyId != null) {
                    DailyAttendance absent = DailyAttendance.builder()
                            .memberId(memberId)
                            .companyId(companyId)
                            .attendanceDate(yesterday)
                            .status(AttendanceStatus.ABSENT)
                            .build();

                    dailyAttendanceRepository.save(absent);
                    createdCount++;
                }
            }

            log.info(">>> [결근 처리 배치 완료] 총 {}명의 직원을 결근 처리했습니다.", createdCount);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * 특정 기간에 승인된 휴가를 가진 직원 ID 목록 조회
     */
    private List<UUID> getMemberIdsWithApprovedLeave(LocalDateTime start, LocalDateTime end) {
        // Request 테이블에서 승인된 휴가 조회
        // 휴가 기간이 대상 날짜와 겹치는 것만
        List<Request> approvedRequests = requestRepository.findAll().stream()
                .filter(r -> r.getStatus() == RequestStatus.APPROVED)
                .filter(r -> r.getPolicy() != null) // 휴가/출장 신청만 (디바이스 제외)
                .filter(r -> {
                    // 요청 기간이 대상 날짜와 겹치는지 확인
                    return !r.getEndDateTime().isBefore(start) && !r.getStartDateTime().isAfter(end);
                })
                .toList();

        return approvedRequests.stream()
                .map(Request::getMemberId)
                .distinct()
                .collect(Collectors.toList());
    }

    // ==================== Job 2: 승인된 휴가 DailyAttendance 생성 ====================

    @Bean
    public Job createApprovedLeaveDailyAttendanceJob() {
        return new JobBuilder("createApprovedLeaveDailyAttendanceJob", jobRepository)
                .start(createApprovedLeaveDailyAttendanceStep())
                .build();
    }

    @Bean
    public Step createApprovedLeaveDailyAttendanceStep() {
        return new StepBuilder("createApprovedLeaveDailyAttendanceStep", jobRepository)
                .tasklet(createApprovedLeaveDailyAttendanceTasklet(), transactionManager)
                .build();
    }

    /**
     * 승인된 휴가의 DailyAttendance 생성 Tasklet
     * 오늘 시작하는 승인된 휴가를 DailyAttendance로 생성
     */
    @Bean
    @Transactional
    public Tasklet createApprovedLeaveDailyAttendanceTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate today = LocalDate.now();
            log.info(">>> [휴가 DailyAttendance 생성 배치 시작] 대상 날짜: {}", today);

            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);

            // 1. 오늘 기간에 포함되는 승인된 휴가 조회
            List<Request> approvedLeaveRequests = requestRepository.findAll().stream()
                    .filter(r -> r.getStatus() == RequestStatus.APPROVED)
                    .filter(r -> r.getPolicy() != null && r.getPolicy().getPolicyType().isBalanceDeductible())
                    .filter(r -> !r.getEndDateTime().isBefore(startOfDay) && !r.getStartDateTime().isAfter(endOfDay))
                    .toList();

            log.info(">>> 오늘 해당하는 승인된 휴가 수: {}", approvedLeaveRequests.size());

            // 2. 각 휴가에 대해 DailyAttendance 생성 (이미 있으면 스킵)
            int createdCount = 0;
            for (Request request : approvedLeaveRequests) {
                UUID memberId = request.getMemberId();

                // 이미 DailyAttendance가 있는지 확인
                Optional<DailyAttendance> existing = dailyAttendanceRepository
                        .findByMemberIdAndAttendanceDate(memberId, today);

                if (existing.isEmpty()) {
                    // DailyAttendance 생성
                    AttendanceStatus leaveStatus = mapPolicyTypeToAttendanceStatus(request);

                    DailyAttendance dailyAttendance = DailyAttendance.builder()
                            .memberId(memberId)
                            .companyId(request.getPolicy().getCompanyId())
                            .attendanceDate(today)
                            .status(leaveStatus)
                            .build();

                    dailyAttendanceRepository.save(dailyAttendance);
                    createdCount++;
                    log.info(">>> 휴가 DailyAttendance 생성: memberId={}, status={}", memberId, leaveStatus);
                }
            }

            log.info(">>> [휴가 DailyAttendance 생성 배치 완료] 총 {}건 생성", createdCount);
            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Request의 PolicyType을 AttendanceStatus로 매핑
     */
    private AttendanceStatus mapPolicyTypeToAttendanceStatus(Request request) {
        return switch (request.getPolicy().getPolicyType().getTypeCode()) {
            case ANNUAL_LEAVE -> AttendanceStatus.ANNUAL_LEAVE;
            case MATERNITY_LEAVE -> AttendanceStatus.MATERNITY_LEAVE;
            case PATERNITY_LEAVE -> AttendanceStatus.PATERNITY_LEAVE;
            case CHILDCARE_LEAVE -> AttendanceStatus.CHILDCARE_LEAVE;
            case FAMILY_CARE_LEAVE -> AttendanceStatus.FAMILY_CARE_LEAVE;
            case MENSTRUAL_LEAVE -> AttendanceStatus.MENSTRUAL_LEAVE;
            case BUSINESS_TRIP -> AttendanceStatus.BUSINESS_TRIP;
            default -> AttendanceStatus.ANNUAL_LEAVE; // 기본값
        };
    }

    // ==================== Job 3: 미완료 퇴근 자동 처리 ====================

    @Bean
    public Job autoCompleteClockOutJob() {
        return new JobBuilder("autoCompleteClockOutJob", jobRepository)
                .start(autoCompleteClockOutStep())
                .build();
    }

    @Bean
    public Step autoCompleteClockOutStep() {
        return new StepBuilder("autoCompleteClockOutStep", jobRepository)
                .tasklet(autoCompleteClockOutTasklet(), transactionManager)
                .build();
    }

    /**
     * 미완료 퇴근 자동 처리 Tasklet
     * 전날 출근했지만 퇴근하지 않은 직원을 자동으로 퇴근 처리
     */
    @Bean
    @Transactional
    public Tasklet autoCompleteClockOutTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info(">>> [미완료 퇴근 자동 처리 배치 시작] 대상 날짜: {}", yesterday);

            // 1. 전날 출근했지만 퇴근하지 않은 DailyAttendance 조회
            List<DailyAttendance> incompleteAttendances = dailyAttendanceRepository
                    .findAllByDateRange(yesterday, yesterday)
                    .stream()
                    .filter(da -> da.getFirstClockIn() != null && da.getLastClockOut() == null)
                    .filter(da -> da.getStatus() == AttendanceStatus.NORMAL_WORK || da.getStatus() == AttendanceStatus.BUSINESS_TRIP)
                    .toList();

            log.info(">>> 미완료 퇴근 대상: {}건", incompleteAttendances.size());

            if (incompleteAttendances.isEmpty()) {
                log.info(">>> [미완료 퇴근 자동 처리 배치 완료] 처리할 대상이 없습니다.");
                return RepeatStatus.FINISHED;
            }

            // 2. 각 DailyAttendance에 대해 자동 퇴근 처리
            int completedCount = 0;
            for (DailyAttendance attendance : incompleteAttendances) {
                try {
                    // 정규 퇴근 시간 계산: 출근 시간 + 9시간 (8시간 근무 + 1시간 휴게)
                    // 더 정확한 처리를 위해서는 Policy를 조회해야 하지만, 배치에서는 기본값 사용
                    LocalDateTime autoClockOutTime = attendance.getFirstClockIn().plusHours(9);

                    // 자정을 넘지 않도록 제한
                    LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);
                    if (autoClockOutTime.isAfter(endOfDay)) {
                        autoClockOutTime = endOfDay;
                    }

                    // 휴일 여부 확인 (주말만 간단히 체크)
                    boolean isHoliday = yesterday.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                            || yesterday.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;

                    // 기본 근무 시간: 480분 (8시간)
                    Integer standardWorkMinutes = 480;

                    // 퇴근 처리 (자동으로 휴게시간 60분 차감)
                    if (attendance.getTotalBreakMinutes() == null || attendance.getTotalBreakMinutes() == 0) {
                        attendance.setTotalBreakMinutes(60); // 기본 휴게시간 1시간
                    }

                    attendance.updateClockOut(autoClockOutTime, standardWorkMinutes, isHoliday);
                    dailyAttendanceRepository.save(attendance);

                    completedCount++;
                    log.info(">>> 자동 퇴근 처리: memberId={}, 출근={}, 자동퇴근={}",
                            attendance.getMemberId(),
                            attendance.getFirstClockIn(),
                            autoClockOutTime);

                } catch (Exception e) {
                    log.error(">>> 자동 퇴근 처리 실패: memberId={}, error={}",
                            attendance.getMemberId(), e.getMessage(), e);
                }
            }

            log.info(">>> [미완료 퇴근 자동 처리 배치 완료] 총 {}건 처리", completedCount);
            return RepeatStatus.FINISHED;
        };
    }

    // ==================== Job 4: 연차 자동 발생 ====================

    @Bean
    public Job annualLeaveAccrualJob() {
        return new JobBuilder("annualLeaveAccrualJob", jobRepository)
                .start(annualLeaveAccrualStep())
                .build();
    }

    @Bean
    public Step annualLeaveAccrualStep() {
        return new StepBuilder("annualLeaveAccrualStep", jobRepository)
                .tasklet(annualLeaveAccrualTasklet(), transactionManager)
                .build();
    }

    /**
     * 연차 자동 발생 Tasklet
     * 매월 1일 또는 매년 1월 1일에 실행하여 직원들에게 연차 발생
     */
    @Bean
    @Transactional
    public Tasklet annualLeaveAccrualTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate today = LocalDate.now();
            log.info(">>> [연차 자동 발생 배치 시작] 실행 날짜: {}", today);

            try {
                // 1. 모든 회사 ID 조회 (Policy에서 중복 제거)
                // DailyAttendance 대신 Policy 테이블에서 조회하여 신규 직원도 포함
                List<UUID> companyIds = policyRepository.findAll().stream()
                        .map(policy -> policy.getCompanyId())
                        .distinct()
                        .toList();

                log.info(">>> 연차 발생 대상 회사 수: {}", companyIds.size());

                if (companyIds.isEmpty()) {
                    log.warn(">>> [연차 자동 발생 배치 완료] 대상 회사 없음 - Policy가 등록되지 않았습니다.");
                    return RepeatStatus.FINISHED;
                }

                // 2. 각 회사별로 연차 발생 처리
                int successCompanyCount = 0;
                int failedCompanyCount = 0;

                for (UUID companyId : companyIds) {
                    try {
                        // 매월 1일: 1년 미만 근로자 월별 연차 발생
                        if (today.getDayOfMonth() == 1) {
                            annualLeaveAccrualService.monthlyAccrualForFirstYearEmployees(companyId, today);
                        }

                        // 매년 1월 1일: 전체 직원 연차 발생 (1년 이상 근로자 포함)
                        if (today.getMonthValue() == 1 && today.getDayOfMonth() == 1) {
                            annualLeaveAccrualService.accrueAnnualLeaveForCompany(companyId, today);
                        }

                        successCompanyCount++;

                    } catch (Exception e) {
                        log.error(">>> 회사 연차 발생 실패: companyId={}", companyId, e);
                        failedCompanyCount++;
                    }
                }

                log.info(">>> [연차 자동 발생 배치 완료] 성공={}, 실패={}", successCompanyCount, failedCompanyCount);
                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                log.error(">>> 연차 자동 발생 배치 실행 중 오류", e);
                throw e;
            }
        };
    }
}
