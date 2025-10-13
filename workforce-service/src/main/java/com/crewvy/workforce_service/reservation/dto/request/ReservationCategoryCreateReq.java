package com.crewvy.workforce_service.reservation.dto.request;

import com.crewvy.workforce_service.reservation.entity.ReservationCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCategoryCreateReq {

    private String name;
    private UUID companyId;

    public ReservationCategory toEntity() {
        return ReservationCategory.builder()
                .name(name)
                .companyId(companyId)
                .build();
    }
}
