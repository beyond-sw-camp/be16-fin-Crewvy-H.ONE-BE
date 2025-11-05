package com.crewvy.workforce_service.salary.repository;

public interface IncomeTaxRepositoryCustom {
    Long findTaxAmount(long taxableIncome, int dependentCount);
}
