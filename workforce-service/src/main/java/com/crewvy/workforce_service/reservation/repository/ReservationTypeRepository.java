package com.crewvy.workforce_service.reservation.repository;

import com.crewvy.workforce_service.reservation.entity.ReservationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationTypeRepository extends JpaRepository<ReservationType, UUID> {

    @EntityGraph(attributePaths = {"reservationCategory"})
    List<ReservationType> findByReservationCategory_CompanyId(UUID companyId);
    
    @EntityGraph(attributePaths = {"reservationCategory"})
    @Query("SELECT rt FROM ReservationType rt " +
           "JOIN rt.reservationCategory rc " +
           "WHERE rc.companyId = :companyId")
    List<ReservationType> findByCompanyId(@Param("companyId") UUID companyId);
}
