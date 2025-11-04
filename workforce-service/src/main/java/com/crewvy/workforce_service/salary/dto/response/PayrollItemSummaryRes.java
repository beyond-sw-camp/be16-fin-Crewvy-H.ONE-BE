package com.crewvy.workforce_service.salary.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PayrollItemSummaryRes {

    private List<ItemTotalRes> paymentItems;
    private List<ItemTotalRes> deductionItems;
}
