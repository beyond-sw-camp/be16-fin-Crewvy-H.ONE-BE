package com.crewvy.workforce_service.reservation.dto.request;

import com.crewvy.workforce_service.reservation.constant.ReservationCategoryStatus;
import com.crewvy.workforce_service.reservation.entity.ReservationCategory;
import com.crewvy.workforce_service.reservation.entity.ReservationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationTypeCreateReq {

    private ReservationCategory reservationCategory;
    private String name;
    private String location;
    private int capacity;
    private String facilities;
    private String description;

    public ReservationType toEntity() {
        return ReservationType.builder()
                .reservationCategory(reservationCategory)
                .reservationCategoryStatus(ReservationCategoryStatus.AVAILABLE)
                .name(name)
                .location(location)
                .capacity(capacity)
                .facilities(facilities)
                .description(description)
                .build();
    }

}
