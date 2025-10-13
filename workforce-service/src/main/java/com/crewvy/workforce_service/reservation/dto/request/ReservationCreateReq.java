package com.crewvy.workforce_service.reservation.dto.request;

import com.crewvy.workforce_service.reservation.constant.ReservationStatus;
import com.crewvy.workforce_service.reservation.entity.Reservation;
import com.crewvy.workforce_service.reservation.entity.ReservationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreateReq {

    private UUID reservationTypeId;
    private UUID memberId;
    private UUID companyId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public Reservation toEntity(ReservationType reservationType) {
        return Reservation.builder()
                .reservationType(reservationType)
                .memberId(memberId)
                .companyId(companyId)
                .status(ReservationStatus.REQUEST)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .build();
    }
}


