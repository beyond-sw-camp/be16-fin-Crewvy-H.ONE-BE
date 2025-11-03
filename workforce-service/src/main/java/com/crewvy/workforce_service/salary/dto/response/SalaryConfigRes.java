package com.crewvy.workforce_service.salary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryConfigRes {

    private UUID memberId;
    private String sabun;
    private String memberName;
    private String department;
    private int baseSalary;
    private List<FixedAllowanceRes> fixedAllowanceList;
}
