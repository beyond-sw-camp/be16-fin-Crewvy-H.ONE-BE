package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.aop.CheckPermission;
import com.crewvy.workforce_service.salary.constant.PeriodMonthType;
import com.crewvy.workforce_service.salary.dto.request.SalaryPolicyCreateReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryPolicyUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.PayPeriodRes;
import com.crewvy.workforce_service.salary.dto.response.SalaryPolicyRes;
import com.crewvy.workforce_service.salary.entity.SalaryPolicy;
import com.crewvy.workforce_service.salary.repository.SalaryPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SalaryPolicyService {

    private final SalaryPolicyRepository salaryPolicyRepository;
    private final HolidayService holidayService;

    @Transactional(readOnly = true)
    public SalaryPolicyRes getSalaryPolicy(UUID companyId) {
        SalaryPolicy salaryPolicy = salaryPolicyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("설정된 급여 정보가 없습니다."));

        return SalaryPolicyRes.fromEntity(salaryPolicy);
    }

    @Transactional
    @CheckPermission(resource = "salary", action = "UPDATE", scope = "COMPANY")
    public SalaryPolicyRes updateSalaryPolicy(SalaryPolicyUpdateReq salaryPolicyUpdateReq) {

        UUID companyId = salaryPolicyUpdateReq.getCompanyId();

        Optional<SalaryPolicy> existingPolicy = salaryPolicyRepository.findByCompanyId(companyId);

        if (existingPolicy.isPresent()) {
            SalaryPolicy policy = existingPolicy.get();
            policy.update(salaryPolicyUpdateReq);
            return SalaryPolicyRes.fromEntity(policy);
        } else {
            SalaryPolicy newPolicy = salaryPolicyUpdateReq.toEntity();
            return SalaryPolicyRes.fromEntity(salaryPolicyRepository.save(newPolicy));
        }
    }

    public SalaryPolicy getLatestSalaryHistoryForCalculation(UUID companyId) {

        return salaryPolicyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("설정된 급여 정보가 없습니다."));
    }

    // 산정 종료일 계산 (산정 종료일 계산)
    public PayPeriodRes calculatePeriodEndDate(SalaryPolicy salaryPolicy, YearMonth yearMonth) {
        LocalDate startDate;
        LocalDate endDate;
        switch (salaryPolicy.getPeriodType()) {
            case LAST_MONTH_FULL:
                // yearMonth 기준 전원 말일
                endDate = yearMonth.minusMonths(1).atEndOfMonth();
                startDate = yearMonth.minusMonths(1).atDay(1);
                return new PayPeriodRes(startDate, endDate);


            case THIS_MONTH_FULL:
                // yearMonth 의 말일 반환
                endDate = yearMonth.atEndOfMonth();
                startDate = yearMonth.atDay(1);
                return new PayPeriodRes(startDate, endDate);

            case SPECIFIC:
                if (salaryPolicy.getPeriodEndMonthType() == PeriodMonthType.CURRENT_MONTH) {
                    endDate = yearMonth.atDay(salaryPolicy.getPeriodEndDay());
                } else {
                    endDate = yearMonth.minusMonths(1).atDay(salaryPolicy.getPeriodEndDay());
                }
                startDate = endDate.minusMonths(1).plusDays(1);

                return new PayPeriodRes(startDate, endDate);

            default:
                throw new IllegalArgumentException("지원하지 않는 급여 산정 타입입니다.");
        }
    }

    public LocalDate getPaymentDate(UUID companyId, YearMonth yearMonth) {
        SalaryPolicy salaryPolicy = salaryPolicyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("설정된 급여 정보가 없습니다."));

        LocalDate paymentDate = switch (salaryPolicy.getPayDayType()) {
            case END_OF_MONTH -> // 말일 지급
                    yearMonth.atEndOfMonth();
            case SPECIFIC_DAY -> // 특정일 지급
                    yearMonth.atDay(salaryPolicy.getPaymentDay());
            default -> throw new IllegalArgumentException("지원하지 않는 지급일 유형입니다.");
        };

        // 휴일 처리 로직
        if (holidayService.isWeekendOrHoliday(paymentDate)) {
            paymentDate = holidayService.adjustForHoliday(paymentDate, salaryPolicy.getHolidayRule());
        }

        return paymentDate;
    }
}
