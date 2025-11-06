package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.entity.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyTypeRepository extends JpaRepository<PolicyType, UUID> {
    Optional<PolicyType> findByCompanyIdAndTypeCode(UUID companyId, PolicyTypeCode typeCode);
    Optional<PolicyType> findByTypeCode(PolicyTypeCode typeCode);
    List<PolicyType> findByCompanyId(UUID companyId);
    long countByCompanyId(UUID companyId);
}