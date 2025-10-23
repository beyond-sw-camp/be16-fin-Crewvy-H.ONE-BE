package com.crewvy.workforce_service.performance.batch;

import com.crewvy.workforce_service.performance.constant.GoalStatus;
import com.crewvy.workforce_service.performance.constant.TeamGoalStatus;
import com.crewvy.workforce_service.performance.repository.PerformanceRepository;
import com.crewvy.workforce_service.performance.repository.TeamGoalRepository;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PerformanceRepository goalRepository;
    private final TeamGoalRepository teamGoalRepository;

    // --- Job 1: 개인 목표(Goal) 상태 변경 ---

    @Bean
    public Job updateGoalStatusJob() {
        return new JobBuilder("updateGoalStatusJob", jobRepository)
                .start(updateGoalStatusStep()) // Job 1의 Step 실행
                .build();
    }

    @Bean
    public Step updateGoalStatusStep() {
        return new StepBuilder("updateGoalStatusStep", jobRepository)
                .tasklet(updateGoalStatusTasklet(), transactionManager)
                .build();
    }

    @Bean
    @Transactional
    public Tasklet updateGoalStatusTasklet() {
        return (contribution, chunkContext) -> {
            log.info(">>> (배치 1 시작) 마감된 '개인 목표' 상태 변경...");

            int updatedCount = goalRepository.updateStatusForExpiredGoals(
                    GoalStatus.APPROVED,
                    GoalStatus.AWAITING_EVALUATION,
                    LocalDate.now()
            );

            log.info(">>> (배치 1 종료) 총 {}건의 '개인 목표' 상태를 '평가 대기'로 변경.", updatedCount);
            return RepeatStatus.FINISHED;
        };
    }

    // --- Job 2: 팀 목표(TeamGoal) 상태 변경 ---

    @Bean
    public Job updateTeamGoalStatusJob() {
        return new JobBuilder("updateTeamGoalStatusJob", jobRepository)
                .start(updateTeamGoalStatusStep()) // Job 2의 Step 실행
                .build();
    }

    @Bean
    public Step updateTeamGoalStatusStep() {
        return new StepBuilder("updateTeamGoalStatusStep", jobRepository)
                .tasklet(updateTeamGoalStatusTasklet(), transactionManager)
                .build();
    }

    @Bean
    @Transactional
    public Tasklet updateTeamGoalStatusTasklet() {
        return (contribution, chunkContext) -> {
            log.info(">>> (배치 2 시작) 마감된 '팀 목표' 상태 변경...");

            int updatedCount = teamGoalRepository.updateStatusForExpiredTeamGoals(
                    TeamGoalStatus.PROCESSING,
                    TeamGoalStatus.AWAITING_EVALUATION,
                    LocalDate.now()
            );

            log.info(">>> (배치 2 종료) 총 {}건의 '팀 목표' 상태를 '평가 대기'로 변경.", updatedCount);
            return RepeatStatus.FINISHED;
        };
    }
}