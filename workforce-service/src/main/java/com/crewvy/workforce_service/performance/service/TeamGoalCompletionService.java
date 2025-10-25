package com.crewvy.workforce_service.performance.service;

import com.crewvy.workforce_service.performance.constant.GoalStatus;
import com.crewvy.workforce_service.performance.constant.TeamGoalStatus;
import com.crewvy.workforce_service.performance.entity.TeamGoal;
import com.crewvy.workforce_service.performance.repository.PerformanceRepository;
import com.crewvy.workforce_service.performance.repository.TeamGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamGoalCompletionService {

    private final PerformanceRepository goalRepository;
    private final TeamGoalRepository teamGoalRepository;

    /**
     * (핵심) 팀 목표 완료 여부를 검사하고 상태를 변경하는 공통 로직
     */
    @Transactional
    public void checkAndCompleteTeamGoal(TeamGoal teamGoal) {
        if (teamGoal == null || teamGoal.getStatus() != TeamGoalStatus.AWAITING_EVALUATION) {
            return; // 이미 완료되었거나 상태가 다르면 종료
        }

        // 1. 아직 평가가 끝나지 않은 '진행형' 상태 정의
        Set<GoalStatus> pendingStatuses = Set.of(
                GoalStatus.AWAITING_EVALUATION,
                GoalStatus.SELF_EVAL_COMPLETED
        );

        // 2. 이 TeamGoal에 속한 Goal 중에 아직 '진행형'인 것이 있는지 DB에 확인
        long pendingGoalsCount = goalRepository.countByTeamGoalAndStatusIn(
                teamGoal,
                pendingStatuses
        );

        // 3. '진행형'인 Goal이 0개라면
        if (pendingGoalsCount == 0) {
            teamGoal.updateStatus(TeamGoalStatus.EVALUATION_COMPLETED);
            teamGoalRepository.save(teamGoal);
            log.info("팀 목표(id: {})가 '평가 완료' 처리되었습니다.", teamGoal.getId());
        }
    }
}