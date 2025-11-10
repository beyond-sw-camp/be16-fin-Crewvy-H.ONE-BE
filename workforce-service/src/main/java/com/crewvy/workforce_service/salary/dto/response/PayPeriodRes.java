package com.crewvy.workforce_service.salary.dto.response;

import lombok.*;

import java.time.LocalDate;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPeriodRes {

    private LocalDate startDate;
    private LocalDate endDate;
}
