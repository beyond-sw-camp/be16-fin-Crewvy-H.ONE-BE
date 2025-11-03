package com.crewvy.workforce_service.salary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemTotalRes {
    private String name;
    private BigInteger totalAmount;
}
