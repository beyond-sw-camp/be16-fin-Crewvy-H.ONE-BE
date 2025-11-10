package com.crewvy.workforce_service.salary.service;

import com.crewvy.workforce_service.salary.repository.IncomeTaxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class IncomeTaxService {

    private final IncomeTaxRepository incomeTaxRepository;

    // 근로 소득 계산
    public double lookupTaxTable(long taxableIncome, int dependentCount) {

        long tableMaxIncome = 10_000_000L;

        if (taxableIncome <= tableMaxIncome) {
            return Optional.ofNullable(
                    incomeTaxRepository.findTaxAmount(taxableIncome, dependentCount)
            ).orElse(0L);

        } else {
            long baseTaxAt10M = Optional.ofNullable(
                    incomeTaxRepository.findTaxAmount(tableMaxIncome, dependentCount)
            ).orElse(0L);

            if (taxableIncome <= 14_000_000L) {
                long excessIncome = taxableIncome - tableMaxIncome;
                return baseTaxAt10M + (excessIncome * 0.98 * 0.35) + 25000;

            } else if (taxableIncome <= 28_000_000L) {
                long excessIncome = taxableIncome - 14_000_000L;
                return baseTaxAt10M + 1_397_000 + (excessIncome * 0.98 * 0.38);

            } else if (taxableIncome <= 30_000_000L) {
                long excessIncome = taxableIncome - 28_000_000L;
                return baseTaxAt10M + 6_610_600 + (excessIncome * 0.98 * 0.40);

            } else if (taxableIncome <= 45_000_000L) {
                long excessIncome = taxableIncome - 30_000_000L;
                return baseTaxAt10M + 7_394_600 + (excessIncome * 0.40);

            } else if (taxableIncome <= 87_000_000L) {
                long excessIncome = taxableIncome - 45_000_000L;
                return baseTaxAt10M + 13_394_600 + (excessIncome * 0.42);

            } else {
                long excessIncome = taxableIncome - 87_000_000L;
                return baseTaxAt10M + 31_034_600 + (excessIncome * 0.45);
            }
        }
    }
}
