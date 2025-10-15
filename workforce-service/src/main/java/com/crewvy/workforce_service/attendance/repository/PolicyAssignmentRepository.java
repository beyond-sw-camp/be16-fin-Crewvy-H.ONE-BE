package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyAssignmentRepository extends JpaRepository<PolicyAssignment, Long> {
}
