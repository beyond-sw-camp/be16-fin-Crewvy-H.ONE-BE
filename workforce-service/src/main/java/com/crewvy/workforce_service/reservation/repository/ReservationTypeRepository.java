package com.crewvy.workforce_service.reservation.repository;

import com.crewvy.workforce_service.reservation.constant.ReservationTypeStatus;
import com.crewvy.workforce_service.reservation.entity.ReservationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationTypeRepository extends JpaRepository<ReservationType, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReservationType> findWithLockById(UUID id);

    List<ReservationType> findByCompanyId(UUID companyId);

    List<ReservationType> findByCompanyIdAndReservationTypeStatusIn(UUID companyId, List<ReservationTypeStatus> statuses);
}
