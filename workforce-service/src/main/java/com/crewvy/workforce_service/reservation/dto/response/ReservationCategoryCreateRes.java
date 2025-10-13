package com.crewvy.workforce_service.reservation.dto.response;

import com.crewvy.workforce_service.reservation.entity.ReservationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationCategoryCreateRes {

    private UUID id;
    private String name;
    private UUID companyId;

    public static ReservationCategoryCreateRes fromEntity(ReservationCategory reservationCategory) {
        return ReservationCategoryCreateRes.builder()
                .id(reservationCategory.getId())
                .name(reservationCategory.getName())
                .companyId(reservationCategory.getCompanyId())
                .build();
    }
}
