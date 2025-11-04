package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PolicyAssignmentRepositoryCustom {

    List<PolicyAssignment> findActiveAssignmentsByTargets(@Param("targetIds") List<UUID> targetIds,
                                                          @Param("companyId") UUID companyId,
                                                          @Param("currentDate") LocalDate currentDate);
}
