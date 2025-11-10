package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.WorkLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkLocationRepository extends JpaRepository<WorkLocation, UUID> {

    /**
     * 회사별 근무지 목록 조회 (페이징)
     */
    Page<WorkLocation> findByCompanyId(UUID companyId, Pageable pageable);

    /**
     * 회사별 활성화된 근무지 목록 조회
     */
    List<WorkLocation> findByCompanyIdAndIsActive(UUID companyId, Boolean isActive);

    /**
     * 회사별 근무지 명칭 중복 체크
     */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM WorkLocation w " +
           "WHERE w.companyId = :companyId " +
           "AND w.name = :name " +
           "AND (:excludeId IS NULL OR w.id != :excludeId)")
    boolean existsByCompanyIdAndName(
            @Param("companyId") UUID companyId,
            @Param("name") String name,
            @Param("excludeId") UUID excludeId
    );

    /**
     * 특정 근무지 조회 (회사 ID로 검증)
     */
    Optional<WorkLocation> findByIdAndCompanyId(UUID id, UUID companyId);

    /**
     * 이름 목록으로 근무지 조회 (출장 정책의 allowedWorkLocations 검증용)
     */
    List<WorkLocation> findByNameIn(List<String> names);
}
