package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.dto.response.ItemTotalRes;

import java.util.List;
import java.util.UUID;

public interface SalaryDetailRepositoryCustom {
    List<ItemTotalRes> sumAmountsBySalaryName(List<UUID> salaryIdList);

}
