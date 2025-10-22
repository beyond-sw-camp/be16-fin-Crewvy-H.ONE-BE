package com.crewvy.workforce_service.performance.repository;

import com.crewvy.workforce_service.performance.entity.TeamGoal;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamGoalRepository extends JpaRepository<TeamGoal, UUID> {
    @Query("SELECT tg FROM TeamGoal tg JOIN tg.teamGoalMembers m WHERE m.memberPositionId = :memberPositionId")
    List<TeamGoal> findAllByMemberPositionId(@Param("memberPositionId") UUID memberPositionId);

    @Query("SELECT tg FROM TeamGoal tg LEFT JOIN FETCH tg.teamGoalMembers WHERE tg.id = :teamGoalId")
    Optional<TeamGoal> findByIdWithMembers(@Param("teamGoalId") UUID teamGoalId);
}
