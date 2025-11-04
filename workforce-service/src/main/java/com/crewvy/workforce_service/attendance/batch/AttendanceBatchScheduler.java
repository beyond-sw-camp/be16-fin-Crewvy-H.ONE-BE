package com.crewvy.workforce_service.attendance.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class AttendanceBatchScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("markAbsentJob")
    private final Job markAbsentJob;

    @Qualifier("createApprovedLeaveDailyAttendanceJob")
    private final Job createApprovedLeaveDailyAttendanceJob;

    @Qualifier("autoCompleteClockOutJob")
    private final Job autoCompleteClockOutJob;

    @Qualifier("annualLeaveAccrualJob")
    private final Job annualLeaveAccrualJob;

    /**
     * 매일 자정 5분에 실행 (결근 처리 + 휴가 DailyAttendance 생성)
     * - 00:05 실행 이유: 자정 직후 실행하면 데이터 정합성 문제 가능
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Async
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
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Async
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
     */
    @Scheduled(cron = "0 0 3 1 * *")
    @Async
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
