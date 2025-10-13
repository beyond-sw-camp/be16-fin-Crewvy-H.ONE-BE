package com.crewvy.workforce_service.reservation.dto.response;

import com.crewvy.workforce_service.reservation.constant.ReservationCategoryStatus;
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
public class ReservationTypeUpdateRes {

    private UUID id;
    private String categoryName;
    private ReservationCategoryStatus reservationCategoryStatus;
    private String name;
    private String location;
    private int capacity;
    private String facilities;
    private String description;

    public static ReservationTypeUpdateRes fromEntity(ReservationType reservationType) {
        return ReservationTypeUpdateRes.builder()
                .id(reservationType.getId())
                .categoryName(reservationType.getReservationCategory().getName())
                .reservationCategoryStatus(reservationType.getReservationCategoryStatus())
                .name(reservationType.getName())
                .location(reservationType.getLocation())
                .capacity(reservationType.getCapacity())
                .facilities(reservationType.getFacilities())
                .description(reservationType.getDescription())
                .build();
    }
}
