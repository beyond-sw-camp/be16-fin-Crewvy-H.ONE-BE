package com.crewvy.workforce_service.reservation.dto.request;

import com.crewvy.workforce_service.reservation.constant.ReservationTypeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationTypeUpdateReq {

    private String name;
    private String location;
    private int capacity;
    private String facilities;
    private String description;
    private ReservationTypeStatus reservationTypeStatus;
}
