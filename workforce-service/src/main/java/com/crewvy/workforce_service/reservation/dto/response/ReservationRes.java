package com.crewvy.workforce_service.reservation.dto.response;

import com.crewvy.common.entity.Bool;
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
    private String name;
    private ReservationStatus status;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String title;
    private int number;
    private String note;
    private int participant;
    private Bool isRepeated;
    private RecurringSettingRes recurringSettingRes;

    public static ReservationRes fromEntity(Reservation reservation) {

        ReservationResBuilder reservationResBuilder = ReservationRes.builder()
                .id(reservation.getId())
                .reservationTypeId(reservation.getReservationType().getId())
                .memberId(reservation.getMemberId())
                .companyId(reservation.getCompanyId())
                .status(reservation.getStatus())
                .startDateTime(reservation.getStartDateTime())
                .endDateTime(reservation.getEndDateTime())
                .title(reservation.getTitle())
                .number(reservation.getNumber())
                .note(reservation.getNote())
                .participant(reservation.getParticipant())
                .isRepeated(reservation.getIsRepeated());

        if (reservation.getRecurringSetting() != null) {
            reservationResBuilder.recurringSettingRes(RecurringSettingRes.fromEntity(reservation.getRecurringSetting()));
        }

        return reservationResBuilder.build();
    }
}


