package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.exception.ResourceNotFoundException;
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
import java.util.UUID;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SalaryPolicyService {

    private final SalaryPolicyRepository salaryPolicyRepository;
    private final HolidayService holidayService;

    public SalaryPolicy createSalaryPolicy(SalaryPolicyCreateReq salaryPolicyCreateReq) {
        if (salaryPolicyRepository.existsByCompanyId(salaryPolicyCreateReq.getCompanyId())) {
            throw new ResourceNotFoundException("이미 존재하는 급여 정책입니다.");
        }

        return salaryPolicyRepository.save(salaryPolicyCreateReq.toEntity());
    }

    @Transactional(readOnly = true)
    public SalaryPolicyRes getSalaryPolicy(UUID companyId) {
        SalaryPolicy salaryPolicy = salaryPolicyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 회사의 급여 정책을 찾을 수 없습니다."));

        return SalaryPolicyRes.fromEntity(salaryPolicy);
    }

    public void updateSalaryPolicy(SalaryPolicyUpdateReq salaryPolicyUpdateReq) {

        SalaryPolicy salaryPolicy = salaryPolicyRepository.findByCompanyId(salaryPolicyUpdateReq.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("해당 회사의 급여 정책을 찾을 수 없습니다."));

        salaryPolicy.update(salaryPolicyUpdateReq);

    }

    public SalaryPolicy getLatestSalaryHistoryForCalculation(UUID companyId) {

        return salaryPolicyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 회사의 급여 정책을 찾을 수 없습니다."));
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
                .orElseThrow(() -> new ResourceNotFoundException("해당 회사의 급여 정책을 찾을 수 없습니다."));

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
