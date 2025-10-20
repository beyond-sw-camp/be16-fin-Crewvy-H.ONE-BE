package com.crewvy.workforce_service.reservation.dto.request;

import com.crewvy.workforce_service.reservation.constant.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdateStatusReq {

    private ReservationStatus reservationStatus;
}
