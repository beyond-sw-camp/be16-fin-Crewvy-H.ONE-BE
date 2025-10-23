package com.crewvy.workforce_service.performance.repository;

import com.crewvy.workforce_service.performance.constant.GoalStatus;
import com.crewvy.workforce_service.performance.entity.Goal;
import com.crewvy.workforce_service.performance.entity.TeamGoal;
import feign.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PerformanceRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByTeamGoal(TeamGoal teamGoal);

    @Query("SELECT g FROM Goal g " +
            "LEFT JOIN FETCH g.teamGoal tg " + // g.teamGoal을 즉시 로딩
            "WHERE g.memberPositionId = :memberPositionId " +
            "AND g.status != :status")
    List<Goal> findActiveGoalsByMemberPositionIdWithTeamGoal(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("status") GoalStatus status
    );

    @Modifying(clearAutomatically = true) // DB 상태 변경 후 1차 캐시를 비워 정합성 보장
    @Transactional // 이 쿼리는 트랜잭션 내에서 실행되어야 함
    @Query("UPDATE Goal g " +
            "SET g.status = :newStatus " +
            "WHERE g.endDate < :today " +
            "AND g.status = :oldStatus")
    int updateStatusForExpiredGoals(@Param("oldStatus") GoalStatus oldStatus,
                                    @Param("newStatus") GoalStatus newStatus,
                                    @Param("today") LocalDate today);
}
