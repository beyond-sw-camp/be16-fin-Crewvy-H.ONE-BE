package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {
}
