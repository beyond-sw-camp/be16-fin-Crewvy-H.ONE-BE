package com.crewvy.workforce_service.reservation.dto.response;

import com.crewvy.workforce_service.reservation.constant.ReservationCategoryStatus;
import com.crewvy.workforce_service.reservation.constant.ReservationStatus;
import com.crewvy.workforce_service.reservation.entity.ReservationCategory;
import com.crewvy.workforce_service.reservation.entity.ReservationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationTypeCreateRes {

    private UUID id;
    private ReservationCategory reservationCategory;
    private ReservationCategoryStatus reservationCategoryStatus;
    private String name;
    private ReservationStatus reservationStatus;
    private String location;
    private int capacity;
    private String facilities;
    private String description;

    public static ReservationTypeCreateRes fromEntity(ReservationType reservationType) {
        return ReservationTypeCreateRes.builder()
                .id(reservationType.getId())
                .name(reservationType.getName())
                .reservationCategory(reservationType.getReservationCategory())
                .location(reservationType.getLocation())
                .capacity(reservationType.getCapacity())
                .reservationCategoryStatus(reservationType.getReservationCategoryStatus())
                .facilities(reservationType.getFacilities())
                .description(reservationType.getDescription())
                .build();
    }
}
