package com.crewvy.workforce_service.salary.config;

import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import com.crewvy.workforce_service.salary.repository.PayrollItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollItemDataSeeder implements ApplicationRunner {

    private final PayrollItemRepository payrollItemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        // 이미 있으면 pass
        if (payrollItemRepository.countByCalculationCodeIsNotNull() > 0) {
            return;
        }

        // 없으면 insert
        // 지급 - 기본급
        PayrollItem baseSalary = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.ALLOWANCE)
                .name("기본급")
                .calculationCode("BASE_SALARY")
                .isActive(Bool.TRUE)
                .isTaxable(Bool.TRUE)
                .description("정규 직원 기본 급여")
                .build();

        // 지급 - 연장 근로 수당
        PayrollItem overtime = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.ALLOWANCE)
                .name("연장근로수당")
                .calculationCode("OVERTIME_ALLOWANCE")
                .isActive(Bool.TRUE)
                .isTaxable(Bool.TRUE)
                .description("정규 근무 시간 초과시 발생하는 수당")
                .build();


        // 지급 - 야간 근로 수당
        PayrollItem nightWork = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.ALLOWANCE)
                .name("야간근로수당")
                .calculationCode("NIGHT_WORK_ALLOWANCE")
                .isActive(Bool.TRUE)
                .isTaxable(Bool.TRUE)
                .description("야간 근로(22시~06시) 시 발생하는 수당")
                .build();


        // 지급 - 휴일 근로 수당
        PayrollItem holidayWork = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.ALLOWANCE)
                .name("휴일근로수당")
                .calculationCode("HOLIDAY_WORK_ALLOWANCE")
                .isActive(Bool.TRUE)
                .isTaxable(Bool.TRUE)
                .description("유급 휴일 근로 시 발생하는 수당")
                .build();

        // 공제 - 국민 연금
        PayrollItem nationalPension = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.DEDUCTION)
                .name("국민연금")
                .calculationCode("NATIONAL_PENSION")
                .isActive(Bool.TRUE)
                .description("국민연금 보험료 공제")
                .build();

        // 공제  - 건강 보험
        PayrollItem healthInsurance = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.DEDUCTION)
                .name("건강보험")
                .calculationCode("HEALTH_INSURANCE")
                .isActive(Bool.TRUE)
                .description("건강보험료 공제")
                .build();

        // 공제 - 고용 보험
        PayrollItem employmentInsurance = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.DEDUCTION)
                .name("고용보험")
                .calculationCode("EMPLOYMENT_INSURANCE")
                .isActive(Bool.TRUE)
                .description("고용보험료 공제")
                .build();

        // 공제 - 장기 요양 보험
        PayrollItem longTermCareInsurance = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.DEDUCTION)
                .name("장기요양보험료")
                .calculationCode("LONG_TERM_CARE_INSURANCE")
                .isActive(Bool.TRUE)
                .description("장기요양보험료 공제")
                .build();

        // 공제 - 근로 소득세
        PayrollItem incomeTax = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.DEDUCTION)
                .name("근로소득세")
                .calculationCode("INCOME_TAX")
                .isActive(Bool.TRUE)
                .description("근로소득세 공제")
                .build();

        // 공제 - 지방 소득세
        PayrollItem localIncomeTax = PayrollItem.builder()
                .companyId(null)
                .salaryType(SalaryType.DEDUCTION)
                .name("지방소득세")
                .calculationCode("LOCAL_INCOME_TAX")
                .isActive(Bool.TRUE)
                .description("지방소득세 공제")
                .build();

        payrollItemRepository.saveAll(List.of(baseSalary, overtime, nightWork, holidayWork
                                            , nationalPension, healthInsurance, employmentInsurance
                                            , longTermCareInsurance, incomeTax, localIncomeTax));

    }
}
