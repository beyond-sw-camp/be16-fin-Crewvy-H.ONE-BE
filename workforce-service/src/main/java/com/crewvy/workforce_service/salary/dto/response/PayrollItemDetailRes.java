package com.crewvy.workforce_service.salary.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;
import java.util.UUID;

@Data
@Builder
public class PayrollItemDetailRes {
    private UUID memberId;
    private String memberName;
    private String sabun;
    private String department;
    private String role;
    private BigInteger amount;
}
