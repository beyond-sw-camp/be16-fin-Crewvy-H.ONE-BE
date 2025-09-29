package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {
}
