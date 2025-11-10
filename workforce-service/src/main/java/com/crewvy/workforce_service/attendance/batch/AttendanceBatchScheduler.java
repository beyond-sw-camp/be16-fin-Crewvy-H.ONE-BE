package com.crewvy.workforce_service.attendance.batch;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AttendanceBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job markAbsentJob;
    private final Job createApprovedLeaveDailyAttendanceJob;
    private final Job autoCompleteClockOutJob;
    private final Job annualLeaveAccrualJob;

    public AttendanceBatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("markAbsentJob") Job markAbsentJob,
            @Qualifier("createApprovedLeaveDailyAttendanceJob") Job createApprovedLeaveDailyAttendanceJob,
            @Qualifier("autoCompleteClockOutJob") Job autoCompleteClockOutJob,
            @Qualifier("annualLeaveAccrualJob") Job annualLeaveAccrualJob
    ) {
        this.jobLauncher = jobLauncher;
        this.markAbsentJob = markAbsentJob;
        this.createApprovedLeaveDailyAttendanceJob = createApprovedLeaveDailyAttendanceJob;
        this.autoCompleteClockOutJob = autoCompleteClockOutJob;
        this.annualLeaveAccrualJob = annualLeaveAccrualJob;
    }

    /**
     * 매일 자정 5분에 실행 (결근 처리 + 휴가 DailyAttendance 생성)
     * - 00:05 실행 이유: 자정 직후 실행하면 데이터 정합성 문제 가능
     * - ShedLock: MSA 환경에서 한 인스턴스만 실행 보장
     */
    @Scheduled(cron = "0 5 0 * * *")
    @SchedulerLock(
            name = "daily_attendance_batch_lock",
            lockAtMostFor = "10m",  // 최대 10분 (비정상 종료 대비)
            lockAtLeastFor = "2m"   // 최소 2분 (과도한 재실행 방지)
    )
    @Async
    @SchedulerLock(
            name = "runDailyAttendanceBatch", // ★ 중요: 작업별로 고유한 이름 지정
            lockAtMostFor = "PT10M",  // 작업이 10분 이상 걸리면 강제 잠금 해제
            lockAtLeastFor = "PT30S"  // 작업이 빨리 끝나도 최소 30초간 잠금 유지
    )
    public void runDailyAttendanceBatch() {
        log.info("========================================");
        log.info("근태 일일 배치 시작: {}", LocalDateTime.now());
        log.info("========================================");

        String runTime = LocalDateTime.now().toString();

        // Job 1: 결근 자동 처리
        try {
            JobParameters markAbsentParams = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            log.info("[배치 1/2] 결근 처리 배치 실행 중...");
            jobLauncher.run(markAbsentJob, markAbsentParams);
            log.info("[배치 1/2] 결근 처리 배치 완료");
        } catch (Exception e) {
            log.error("[배치 1/2] 결근 처리 배치 실행 중 오류 발생", e);
        }

        // Job 2: 승인된 휴가 DailyAttendance 생성
        try {
            JobParameters createLeaveParams = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            log.info("[배치 2/2] 휴가 DailyAttendance 생성 배치 실행 중...");
            jobLauncher.run(createApprovedLeaveDailyAttendanceJob, createLeaveParams);
            log.info("[배치 2/2] 휴가 DailyAttendance 생성 배치 완료");
        } catch (Exception e) {
            log.error("[배치 2/2] 휴가 DailyAttendance 생성 배치 실행 중 오류 발생", e);
        }

        log.info("========================================");
        log.info("근태 일일 배치 완료: {}", LocalDateTime.now());
        log.info("========================================");
    }

    /**
     * 수동 실행용 - 테스트 목적
     * 필요시 REST API로 호출 가능
     */
    public void runMarkAbsentJobManually() {
        log.info("수동 실행: 결근 처리 배치");
        String runTime = LocalDateTime.now().toString();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            jobLauncher.run(markAbsentJob, params);
            log.info("수동 실행 완료: 결근 처리 배치");
        } catch (Exception e) {
            log.error("수동 실행 중 오류 발생: 결근 처리 배치", e);
        }
    }

    /**
     * 수동 실행용 - 테스트 목적
     */
    @Async
    public void runCreateLeaveDailyAttendanceJobManually() {
        log.info("수동 실행: 휴가 DailyAttendance 생성 배치");
        String runTime = LocalDateTime.now().toString();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            jobLauncher.run(createApprovedLeaveDailyAttendanceJob, params);
            log.info("수동 실행 완료: 휴가 DailyAttendance 생성 배치");
        } catch (Exception e) {
            log.error("수동 실행 중 오류 발생: 휴가 DailyAttendance 생성 배치", e);
        }
    }

    /**
     * 매일 새벽 2시에 실행 (미완료 퇴근 자동 처리)
     * - 02:00 실행 이유: 전날 퇴근하지 않은 직원을 자동 퇴근 처리 (결근 처리보다 먼저)
     * - ShedLock: MSA 환경에서 한 인스턴스만 실행 보장
     */
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(
            name = "auto_complete_clock_out_lock",
            lockAtMostFor = "15m",  // 최대 15분
            lockAtLeastFor = "3m"   // 최소 3분
    )
    @Async
    @SchedulerLock(
            name = "runAutoCompleteClockOutBatch", // ★ 중요: 작업별로 고유한 이름 지정
            lockAtMostFor = "PT10M",  // 작업이 10분 이상 걸리면 강제 잠금 해제
            lockAtLeastFor = "PT30S"  // 작업이 빨리 끝나도 최소 30초간 잠금 유지
    )
    public void runAutoCompleteClockOutBatch() {
        log.info("========================================");
        log.info("미완료 퇴근 자동 처리 배치 시작: {}", LocalDateTime.now());
        log.info("========================================");

        String runTime = LocalDateTime.now().toString();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            jobLauncher.run(autoCompleteClockOutJob, params);
            log.info("========================================");
            log.info("미완료 퇴근 자동 처리 배치 완료: {}", LocalDateTime.now());
            log.info("========================================");

        } catch (Exception e) {
            log.error("========================================");
            log.error("미완료 퇴근 자동 처리 배치 실행 중 오류 발생", e);
            log.error("========================================");
        }
    }

    /**
     * 수동 실행용 - 테스트 목적
     */
    @Async
    public void runAutoCompleteClockOutJobManually() {
        log.info("수동 실행: 미완료 퇴근 자동 처리 배치");
        String runTime = LocalDateTime.now().toString();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            jobLauncher.run(autoCompleteClockOutJob, params);
            log.info("수동 실행 완료: 미완료 퇴근 자동 처리 배치");
        } catch (Exception e) {
            log.error("수동 실행 중 오류 발생: 미완료 퇴근 자동 처리 배치", e);
        }
    }

    // ==================== Job 4: 연차 자동 발생 ====================

    /**
     * 매월 1일 새벽 3시에 실행 (연차 자동 발생)
     * - 1년 미만 근로자: 매월 1일 추가 발생
     * - 1년 이상 근로자: 매년 1월 1일 연차 발생
     * - ShedLock: MSA 환경에서 한 인스턴스만 실행 보장
     */
    @Scheduled(cron = "0 0 3 1 * *")
    @SchedulerLock(
            name = "annual_leave_accrual_lock",
            lockAtMostFor = "30m",  // 최대 30분 (직원 수 많을 경우 대비)
            lockAtLeastFor = "5m"   // 최소 5분
    )
    @Async
    @SchedulerLock(
            name = "runAnnualLeaveAccrualBatch", // ★ 중요: 작업별로 고유한 이름 지정
            lockAtMostFor = "PT10M",  // 작업이 10분 이상 걸리면 강제 잠금 해제
            lockAtLeastFor = "PT30S"  // 작업이 빨리 끝나도 최소 30초간 잠금 유지
    )
    public void runAnnualLeaveAccrualBatch() {
        log.info("========================================");
        log.info("연차 자동 발생 배치 시작: {}", LocalDateTime.now());
        log.info("========================================");

        String runTime = LocalDateTime.now().toString();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            jobLauncher.run(annualLeaveAccrualJob, params);
            log.info("========================================");
            log.info("연차 자동 발생 배치 완료: {}", LocalDateTime.now());
            log.info("========================================");

        } catch (Exception e) {
            log.error("========================================");
            log.error("연차 자동 발생 배치 실행 중 오류 발생", e);
            log.error("========================================");
        }
    }

    /**
     * 수동 실행용 - 테스트 목적
     */
    @Async
    public void runAnnualLeaveAccrualJobManually() {
        log.info("수동 실행: 연차 자동 발생 배치");
        String runTime = LocalDateTime.now().toString();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            jobLauncher.run(annualLeaveAccrualJob, params);
            log.info("수동 실행 완료: 연차 자동 발생 배치");
        } catch (Exception e) {
            log.error("수동 실행 중 오류 발생: 연차 자동 발생 배치", e);
        }
    }
}
