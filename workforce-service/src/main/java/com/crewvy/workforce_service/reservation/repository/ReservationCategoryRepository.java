package com.crewvy.workforce_service.reservation.repository;

import com.crewvy.workforce_service.reservation.entity.ReservationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationCategoryRepository extends JpaRepository<ReservationCategory, UUID> {
    List<ReservationCategory> findByCompanyId(UUID companyId);
}
