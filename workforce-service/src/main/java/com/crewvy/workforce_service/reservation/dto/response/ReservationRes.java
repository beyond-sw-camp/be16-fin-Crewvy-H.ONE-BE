package com.crewvy.workforce_service.reservation.dto.response;

import com.crewvy.workforce_service.reservation.constant.ReservationStatus;
import com.crewvy.workforce_service.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRes {

    private UUID id;
    private UUID reservationTypeId;
    private UUID memberId;
    private UUID companyId;
    private ReservationStatus status;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public static ReservationRes fromEntity(Reservation reservation) {
        return ReservationRes.builder()
                .id(reservation.getId())
                .reservationTypeId(reservation.getReservationType().getId())
                .memberId(reservation.getMemberId())
                .companyId(reservation.getCompanyId())
                .status(reservation.getStatus())
                .startDateTime(reservation.getStartDateTime())
                .endDateTime(reservation.getEndDateTime())
                .build();
    }
}


