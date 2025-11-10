package com.crewvy.workforce_service.reservation.dto.response;

import com.crewvy.workforce_service.reservation.constant.ReservationTypeStatus;
import com.crewvy.workforce_service.reservation.entity.ReservationType;
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
public class ReservationTypeListRes {

    private UUID id;
    private UUID companyId;
    private String categoryName;
    private ReservationTypeStatus reservationTypeStatus;
    private String name;
    private String location;
    private int capacity;
    private String facilities;
    private String description;
    private LocalDateTime createdAt;

    public static ReservationTypeListRes fromEntity(ReservationType reservationType) {
        return ReservationTypeListRes.builder()
                .id(reservationType.getId())
                .categoryName(reservationType.getReservationCategory() != null ? 
                        reservationType.getReservationCategory().getName() : "카테고리 없음")
                .reservationTypeStatus(reservationType.getReservationTypeStatus())
                .name(reservationType.getName())
                .location(reservationType.getLocation())
                .capacity(reservationType.getCapacity())
                .facilities(reservationType.getFacilities())
                .description(reservationType.getDescription())
                .createdAt(reservationType.getCreatedAt())
                .build();
    }
}
