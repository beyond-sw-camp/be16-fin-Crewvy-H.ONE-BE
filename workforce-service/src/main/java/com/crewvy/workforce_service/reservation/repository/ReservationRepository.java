package com.crewvy.workforce_service.reservation.repository;

import com.crewvy.workforce_service.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByCompanyId(UUID companyId);

    List<Reservation> findByCompanyIdAndMemberId(UUID companyId, UUID memberId);

    boolean existsByReservationTypeIdAndStartDateTimeLessThanAndEndDateTimeGreaterThan(UUID reservationTypeId,
                                                                                       LocalDateTime endDateTime,
                                                                                       LocalDateTime startDateTime);
}


