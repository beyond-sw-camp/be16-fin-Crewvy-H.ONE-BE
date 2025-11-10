package com.crewvy.workforce_service.performance.batch;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;

    // Config 파일에 Job이 2개 있으므로, @Qualifier로 실행할 Job을 명시해서 주입받습니다.
    @Qualifier("updateGoalStatusJob")
    private final Job updateGoalStatusJob;

    @Qualifier("updateTeamGoalStatusJob")
    private final Job updateTeamGoalStatusJob;

    @Qualifier("completeTeamGoalEvaluationJob")
    private final Job completeTeamGoalEvaluationJob;

    /**
     * 매일 새벽 1시에 실행
     */
    @Scheduled(cron = "0 10 1 * * *")
    @Async // (권장) 스케줄러 스레드가 아닌 별도 스레드에서 배치를 비동기로 실행
    @SchedulerLock(
            name = "runAllExpiredStatusUpdateJobs", // ★ 중요: 작업별로 고유한 이름 지정
            lockAtMostFor = "PT10M",  // 작업이 10분 이상 걸리면 강제 잠금 해제
            lockAtLeastFor = "PT30S"  // 작업이 빨리 끝나도 최소 30초간 잠금 유지
    )
    public void runAllExpiredStatusUpdateJobs() {
        log.info("스케줄러 실행: 마감된 목표 상태 변경 배치 시작");

        // 매번 다른 파라미터로 실행해야 하므로 타임스탬프를 공통으로 사용
        String runTime = LocalDateTime.now().toString();

        try {
            // --- Job 1 실행 ---
            JobParameters goalJobParams = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            log.info("Job 1 (updateGoalStatusJob) 실행...");
            jobLauncher.run(updateGoalStatusJob, goalJobParams);

            // --- Job 2 실행 (Job 1이 성공적으로 끝나면) ---
            JobParameters teamGoalJobParams = new JobParametersBuilder()
                    .addString("runTime", runTime) // Job 1과 파라미터가 같아도 Job이 다르므로 OK
                    .toJobParameters();

            log.info("Job 2 (updateTeamGoalStatusJob) 실행...");
            jobLauncher.run(updateTeamGoalStatusJob, teamGoalJobParams);

            // --- Job 3 실행 (방금 추가한 Job) ---
            JobParameters evaluationJobParams = new JobParametersBuilder()
                    .addString("runTime", runTime)
                    .toJobParameters();

            log.info("Job 3 (completeTeamGoalEvaluationJob) 실행...");
            jobLauncher.run(completeTeamGoalEvaluationJob, evaluationJobParams);

        } catch (Exception e) {
            log.error("배치 스케줄링 실행 중 오류 발생", e);
        }
    }
}
