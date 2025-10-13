package com.crewvy.workforce_service.salary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryInfoUpdateReq {

    private UUID id;
    private UUID companyId;
    private UUID payrollItemId;
    private BigInteger amount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
