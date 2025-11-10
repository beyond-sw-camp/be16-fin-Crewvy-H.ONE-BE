package com.crewvy.workforce_service.salary.dto.request;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayrollItemUpdateReq {

    private UUID id;
    private SalaryType salaryType;
    private String name;
    private Bool isActive;
    private String description;
}
