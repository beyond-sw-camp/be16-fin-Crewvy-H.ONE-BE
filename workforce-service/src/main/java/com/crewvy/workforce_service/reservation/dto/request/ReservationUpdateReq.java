package com.crewvy.workforce_service.reservation.dto.request;

import com.crewvy.workforce_service.reservation.constant.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdateReq {

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private ReservationStatus status;
}


