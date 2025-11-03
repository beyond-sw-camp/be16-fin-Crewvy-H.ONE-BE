package com.crewvy.workforce_service.salary.dto.response;

import lombok.*;

import java.util.UUID;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetPayRes {

    private UUID memberId;
    private long baseSalary;
    private long totalDeduction;
    private long netPay;
}
