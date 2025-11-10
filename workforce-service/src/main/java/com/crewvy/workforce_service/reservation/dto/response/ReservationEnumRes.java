package com.crewvy.workforce_service.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationEnumRes {

    private String statusName;
    private String codeValue;
    private String codeName;
}
