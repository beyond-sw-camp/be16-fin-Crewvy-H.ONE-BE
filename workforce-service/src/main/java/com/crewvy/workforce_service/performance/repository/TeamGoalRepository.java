package com.crewvy.workforce_service.performance.repository;

import com.crewvy.workforce_service.performance.constant.TeamGoalStatus;
import com.crewvy.workforce_service.performance.entity.TeamGoal;
import feign.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamGoalRepository extends JpaRepository<TeamGoal, UUID> {
    @Query("SELECT tg FROM TeamGoal tg JOIN tg.teamGoalMembers m " +
            "WHERE m.memberPositionId = :memberPositionId " +
            "AND tg.status != com.crewvy.workforce_service.performance.constant.TeamGoalStatus.CANCELED")
    List<TeamGoal> findAllByMemberPositionId(@Param("memberPositionId") UUID memberPositionId);

    @Query("SELECT tg FROM TeamGoal tg LEFT JOIN FETCH tg.teamGoalMembers WHERE tg.id = :teamGoalId")
    Optional<TeamGoal> findByIdWithMembers(@Param("teamGoalId") UUID teamGoalId);

    @Query("SELECT tg FROM TeamGoal tg LEFT JOIN FETCH tg.goalList WHERE tg.id = :teamGoalId")
    Optional<TeamGoal> findByIdWithGoals(@Param("teamGoalId") UUID teamGoalId);

    @Query("SELECT tg FROM TeamGoal tg JOIN tg.teamGoalMembers m " +
            "WHERE m.memberPositionId = :memberPositionId " +
            "AND tg.status = :status")
    List<TeamGoal> findAllByMemberPositionIdAndStatus(@Param("memberPositionId") UUID memberPositionId,
                                                      @Param("status") TeamGoalStatus status);

    @Query("SELECT tg FROM TeamGoal tg " +
            "WHERE tg.memberPositionId = :memberPositionId " +
            "AND tg.status = :status")
    List<TeamGoal> findAllByMemberPositionIdAndStatus2(@Param("memberPositionId") UUID memberPositionId,
                                                      @Param("status") TeamGoalStatus status);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE TeamGoal tg " +
            "SET tg.status = :newStatus " +
            "WHERE tg.endDate < :today " +
            "AND tg.status = :oldStatus")
    int updateStatusForExpiredTeamGoals(@Param("oldStatus") TeamGoalStatus oldStatus,
                                        @Param("newStatus") TeamGoalStatus newStatus,
                                        @Param("today") LocalDate today);
}
