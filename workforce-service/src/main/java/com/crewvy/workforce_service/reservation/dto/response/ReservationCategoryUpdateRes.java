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
public class ReservationCategoryUpdateRes {

    private UUID id;
    private String name;
    private UUID companyId;

    public static ReservationCategoryUpdateRes fromEntity(ReservationCategory category) {
        return ReservationCategoryUpdateRes.builder()
                .id(category.getId())
                .name(category.getName())
                .companyId(category.getCompanyId())
                .build();
    }
}


