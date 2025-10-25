package com.crewvy.workforce_service.performance.batch;

import com.crewvy.workforce_service.performance.constant.GoalStatus;
import com.crewvy.workforce_service.performance.constant.TeamGoalStatus;
import com.crewvy.workforce_service.performance.entity.TeamGoal;
import com.crewvy.workforce_service.performance.repository.PerformanceRepository; // (GoalRepository)
import com.crewvy.workforce_service.performance.repository.TeamGoalRepository;
import com.crewvy.workforce_service.performance.service.TeamGoalCompletionService;
import jakarta.persistence.EntityManagerFactory; // (수정 1) import 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PerformanceRepository goalRepository;
    private final TeamGoalRepository teamGoalRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final TeamGoalCompletionService teamGoalCompletionService;

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
                .start(updateTeamGoalStatusStep()) // (수정 2) Job 2의 Step을 정확히 호출
                .build();
    }

    @Bean
    public Step updateTeamGoalStatusStep() { // (수정 2) 메서드 이름 변경
        return new StepBuilder("updateTeamGoalStatusStep", jobRepository)
                .tasklet(updateTeamGoalStatusTasklet(), transactionManager) // (수정 2) Job 2의 Tasklet 호출
                .build();
    }

    @Bean
    @Transactional
    public Tasklet updateTeamGoalStatusTasklet() { // (수정 2) 메서드 이름 변경
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

    // --- Job 3: 모든 하위 목표 평가 완료 시 팀 목표 상태 변경 ---

    @Bean
    public JpaPagingItemReader<TeamGoal> teamGoalAwaitingEvaluationReader() {
        // JPQL 쿼리 작성
        String jpqlQuery = "SELECT tg FROM TeamGoal tg WHERE tg.status = :status";

        // 파라미터 설정
        Map<String, Object> params = new HashMap<>();
        params.put("status", TeamGoalStatus.AWAITING_EVALUATION);

        return new JpaPagingItemReaderBuilder<TeamGoal>()
                .name("teamGoalAwaitingEvaluationReader")
                .entityManagerFactory(entityManagerFactory) // (수정 1) 주입된 EMF 사용
                .queryString(jpqlQuery)
                .parameterValues(params)
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<TeamGoal, TeamGoal> teamGoalEvaluationProcessor() {
        return teamGoal -> {
            // (2) (수정) 배치 Processor는 공통 서비스를 호출하기만 함
            teamGoalCompletionService.checkAndCompleteTeamGoal(teamGoal);
            return null; // Processor는 상태 변경만 하고 Writer로 넘길 필요 없음
        };
    }

    @Bean
    public JpaItemWriter<TeamGoal> teamGoalEvaluationWriter() {
        return new JpaItemWriterBuilder<TeamGoal>()
                .entityManagerFactory(entityManagerFactory) // (수정 1) 주입된 EMF 사용
                .build();
    }

    @Bean
    public Step completeTeamGoalEvaluationStep() {
        return new StepBuilder("completeTeamGoalEvaluationStep", jobRepository)
                .<TeamGoal, TeamGoal>chunk(100, transactionManager)
                .reader(teamGoalAwaitingEvaluationReader())
                .processor(teamGoalEvaluationProcessor())
                .writer(teamGoalEvaluationWriter())
                .build();
    }

    @Bean
    public Job completeTeamGoalEvaluationJob() {
        return new JobBuilder("completeTeamGoalEvaluationJob", jobRepository)
                .start(completeTeamGoalEvaluationStep())
                .build();
    }
}