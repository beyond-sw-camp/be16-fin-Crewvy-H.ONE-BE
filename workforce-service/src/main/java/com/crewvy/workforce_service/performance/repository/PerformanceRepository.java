package com.crewvy.workforce_service.performance.repository;

import com.crewvy.workforce_service.performance.entity.Goal;
import com.crewvy.workforce_service.performance.entity.TeamGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PerformanceRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByTeamGoal(TeamGoal teamGoal);

    List<Goal> findByMemberPositionId(UUID memberPositionId);
}
