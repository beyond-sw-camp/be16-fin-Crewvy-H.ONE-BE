package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberBalanceRepository extends JpaRepository<MemberBalance, UUID> {
}
