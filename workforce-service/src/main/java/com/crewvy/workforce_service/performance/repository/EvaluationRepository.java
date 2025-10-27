package com.crewvy.workforce_service.performance.repository;

import com.crewvy.workforce_service.performance.constant.EvaluationType;
import com.crewvy.workforce_service.performance.entity.Evaluation;
import com.crewvy.workforce_service.performance.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    Optional<Evaluation> findByGoalAndType(Goal goal, EvaluationType type);

    List<Evaluation> findByGoal(Goal goal);
}
