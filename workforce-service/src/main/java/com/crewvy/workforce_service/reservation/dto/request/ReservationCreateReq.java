package com.crewvy.workforce_service.reservation.dto.request;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.reservation.constant.ReservationRequestStatus;
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
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String title;
    private int number;
    private String note;
    private int participant;
    private Bool isRepeated;
    private RepeatCreateReq repeatCreateReq;

    public Reservation toEntity(UUID memberId, UUID companyId, ReservationType reservationType) {
        return toEntity(memberId, companyId, reservationType, this.startDateTime, this.endDateTime);
    }

    public Reservation toEntity(UUID memberId, UUID companyId, ReservationType reservationType,
                                LocalDateTime startDateTime, LocalDateTime endDateTime) {

        return Reservation.builder()
                .reservationType(reservationType)
                .memberId(memberId)
                .companyId(companyId)
                .status(ReservationStatus.BEFORE)
                .requestStatus(ReservationRequestStatus.REQUEST)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .title(title)
                .number(number)
                .note(note)
                .participant(participant)
                .isRepeated(isRepeated)
                .build();
    }
}


