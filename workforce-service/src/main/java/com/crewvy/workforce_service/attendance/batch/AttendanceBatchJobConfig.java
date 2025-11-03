package com.crewvy.workforce_service.attendance.batch;

import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.Request;
import com.crewvy.workforce_service.attendance.repository.DailyAttendanceRepository;
import com.crewvy.workforce_service.attendance.repository.RequestRepository;
import com.crewvy.workforce_service.attendance.service.AnnualLeaveAccrualService;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
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
    private final EntityManagerFactory entityManagerFactory;

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

            // 1. 전날에 승인된 휴가를 가진 직원 목록 조회 (개선)
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);
            List<UUID> memberIdsWithApprovedLeave = requestRepository.findMemberIdsWithApprovedLeaveBetween(RequestStatus.APPROVED, startOfDay, endOfDay);
            log.info(">>> 승인된 휴가가 있는 직원 수: {}", memberIdsWithApprovedLeave.size());

            // 2. 전날 DailyAttendance가 이미 있는 직원 목록 조회
            List<DailyAttendance> existingAttendances = dailyAttendanceRepository.findAllByDateRange(yesterday, yesterday);
            Set<UUID> memberIdsWithAttendance = existingAttendances.stream()
                    .map(DailyAttendance::getMemberId)
                    .collect(Collectors.toSet());
            log.info(">>> 이미 근태 기록이 있는 직원 수: {}", memberIdsWithAttendance.size());

            // 3. 최근 30일 내 출근 기록이 있는 활성 직원 조회 (개선)
            LocalDate thirtyDaysAgo = yesterday.minusDays(30);
            // 이 부분은 모든 활성 직원을 알아내야 하므로, 여전히 많은 데이터를 조회할 수 있습니다.
            // 더 나은 방법은 member-service에서 활성 직원 목록을 가져오는 것입니다.
            // 현재 구조에서는 DailyAttendance에서 활성 유저를 추측합니다.
            List<DailyAttendance> recentAttendances = dailyAttendanceRepository.findAllByDateRange(thirtyDaysAgo, yesterday.minusDays(1));
            Set<UUID> activeMemberIds = recentAttendances.stream()
                    .map(DailyAttendance::getMemberId)
                    .collect(Collectors.toSet());
            log.info(">>> 최근 30일 내 활성 직원 수: {}", activeMemberIds.size());


            // 4. 결근 대상: 활성 직원 중 전날 근태 기록도 없고 승인된 휴가도 없는 직원
            Set<UUID> absentMemberIds = new HashSet<>(activeMemberIds);
            absentMemberIds.removeAll(memberIdsWithAttendance);
            absentMemberIds.removeAll(memberIdsWithApprovedLeave);

            log.info(">>> 결근 처리 대상 직원 수: {}", absentMemberIds.size());

            if (absentMemberIds.isEmpty()) {
                log.info(">>> [결근 처리 배치 완료] 처리할 대상이 없습니다.");
                return RepeatStatus.FINISHED;
            }

            // 5. 결근 처리 - DailyAttendance 생성 (개선)
            List<DailyAttendance> absentAttendances = new ArrayList<>();
            for (UUID memberId : absentMemberIds) {
                dailyAttendanceRepository.findFirstByMemberIdOrderByAttendanceDateDesc(memberId)
                        .ifPresent(latestAttendance -> {
                            DailyAttendance absent = DailyAttendance.builder()
                                    .memberId(memberId)
                                    .companyId(latestAttendance.getCompanyId())
                                    .attendanceDate(yesterday)
                                    .status(AttendanceStatus.ABSENT)
                                    .build();
                            absentAttendances.add(absent);
                        });
            }

            dailyAttendanceRepository.saveAll(absentAttendances);

            log.info(">>> [결근 처리 배치 완료] 총 {}명의 직원을 결근 처리했습니다.", absentAttendances.size());
            return RepeatStatus.FINISHED;
        };
    }


    // ==================== Job 2: 승인된 휴가 DailyAttendance 생성 (개선) ====================

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job createApprovedLeaveDailyAttendanceJob() {
        return new JobBuilder("createApprovedLeaveDailyAttendanceJob", jobRepository)
                .start(createApprovedLeaveDailyAttendanceStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step createApprovedLeaveDailyAttendanceStep(@Value("#{jobParameters[date]}") String date) {
        return new StepBuilder("createApprovedLeaveDailyAttendanceStep", jobRepository)
                .<Request, DailyAttendance>chunk(CHUNK_SIZE, transactionManager)
                .reader(approvedLeaveRequestReader(null))
                .processor(approvedLeaveRequestProcessor())
                .writer(dailyAttendanceWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Request> approvedLeaveRequestReader(@Value("#{jobParameters[date]}") String date) {
        LocalDate today = (date == null) ? LocalDate.now() : LocalDate.parse(date);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        return new JpaPagingItemReaderBuilder<Request>()
                .name("approvedLeaveRequestReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT r FROM Request r " +
                             "WHERE r.status = :status " +
                             "AND r.policy IS NOT NULL AND r.policy.policyType.isBalanceDeductible = true " +
                             "AND r.endDateTime >= :start AND r.startDateTime <= :end")
                .parameterValues(Map.of("status", RequestStatus.APPROVED, "start", startOfDay, "end", endOfDay))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Request, DailyAttendance> approvedLeaveRequestProcessor() {
        return request -> {
            LocalDate today = LocalDate.now(); // Or get from job parameters
            UUID memberId = request.getMemberId();

            // 이미 DailyAttendance가 있는지 확인
            boolean exists = dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today).isPresent();

            if (exists) {
                log.info(">>> 이미 DailyAttendance가 존재하여 건너뜁니다: memberId={}", memberId);
                return null; // Writer로 전달하지 않음
            }

            AttendanceStatus leaveStatus = mapPolicyTypeToAttendanceStatus(request);
            return DailyAttendance.builder()
                    .memberId(memberId)
                    .companyId(request.getPolicy().getCompanyId())
                    .attendanceDate(today)
                    .status(leaveStatus)
                    .build();
        };
    }

    @Bean
    @StepScope
    public ItemWriter<DailyAttendance> dailyAttendanceWriter() {
        return items -> {
            log.info(">>> {}개의 DailyAttendance를 저장합니다.", items.size());
            dailyAttendanceRepository.saveAll(items);
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

    // ==================== Job 3: 미완료 퇴근 자동 처리 (개선) ====================

    @Bean
    public Job autoCompleteClockOutJob() {
        return new JobBuilder("autoCompleteClockOutJob", jobRepository)
                .start(autoCompleteClockOutStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step autoCompleteClockOutStep(@Value("#{jobParameters[date]}") String date) {
        return new StepBuilder("autoCompleteClockOutStep", jobRepository)
                .<DailyAttendance, DailyAttendance>chunk(CHUNK_SIZE, transactionManager)
                .reader(incompleteAttendanceReader(null))
                .processor(incompleteAttendanceProcessor())
                .writer(dailyAttendanceWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<DailyAttendance> incompleteAttendanceReader(@Value("#{jobParameters[date]}") String date) {
        LocalDate yesterday = (date == null) ? LocalDate.now().minusDays(1) : LocalDate.parse(date).minusDays(1);

        return new JpaPagingItemReaderBuilder<DailyAttendance>()
                .name("incompleteAttendanceReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT da FROM DailyAttendance da " +
                             "WHERE da.attendanceDate = :date " +
                             "AND da.firstClockIn IS NOT NULL AND da.lastClockOut IS NULL " +
                             "AND da.status IN (com.crewvy.workforce_service.attendance.constant.AttendanceStatus.NORMAL_WORK, com.crewvy.workforce_service.attendance.constant.AttendanceStatus.BUSINESS_TRIP)")
                .parameterValues(Map.of("date", yesterday))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<DailyAttendance, DailyAttendance> incompleteAttendanceProcessor() {
        return attendance -> {
            LocalDate yesterday = attendance.getAttendanceDate();
            LocalDateTime autoClockOutTime = attendance.getFirstClockIn().plusHours(9);
            LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

            if (autoClockOutTime.isAfter(endOfDay)) {
                autoClockOutTime = endOfDay;
            }

            boolean isHoliday = yesterday.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                    || yesterday.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;

            Integer standardWorkMinutes = 480;

            if (attendance.getTotalBreakMinutes() == null || attendance.getTotalBreakMinutes() == 0) {
                attendance.setTotalBreakMinutes(60);
            }

            attendance.updateClockOut(autoClockOutTime, standardWorkMinutes, isHoliday);
            return attendance;
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
                // 1. 모든 회사 ID 조회 (개선)
                List<UUID> companyIds = policyRepository.findDistinctCompanyIds();

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
