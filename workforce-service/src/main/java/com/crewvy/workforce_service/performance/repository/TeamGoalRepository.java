package com.crewvy.workforce_service.performance.repository;

import com.crewvy.workforce_service.performance.entity.TeamGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TeamGoalRepository extends JpaRepository<TeamGoal, UUID> {
}
