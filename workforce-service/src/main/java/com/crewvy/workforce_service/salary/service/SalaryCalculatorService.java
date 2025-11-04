package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.aop.AuthUser;
import com.crewvy.workforce_service.aop.CheckPermission;
import com.crewvy.workforce_service.attendance.dto.response.DailyAttendanceRes;
import com.crewvy.workforce_service.attendance.service.AttendanceService;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberSalaryListRes;
import com.crewvy.workforce_service.salary.config.PayrollProperties;
import com.crewvy.workforce_service.salary.constant.PayType;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.dto.request.SalaryHistoryListReq;
import com.crewvy.workforce_service.salary.dto.response.FixedAllowanceRes;
import com.crewvy.workforce_service.salary.dto.response.PayPeriodRes;
import com.crewvy.workforce_service.salary.dto.response.SalaryCalculationRes;
import com.crewvy.workforce_service.salary.dto.response.SalaryDetailRes;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import com.crewvy.workforce_service.salary.entity.SalaryHistory;
import com.crewvy.workforce_service.salary.entity.SalaryPolicy;
import com.crewvy.workforce_service.salary.repository.PayrollItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SalaryCalculatorService {

    private final MemberClient memberClient;
    private final SalaryPolicyService salaryPolicyService;
    private final SalaryHistoryService salaryHistoryService;
    private final FixedAllowanceService fixedAllowanceService;
    private final PayrollItemRepository payrollItemRepository;
    private final HolidayService holidayService;
    private final AttendanceService attendanceService;
    private final IncomeTaxService incomeTaxService;

    private final PayrollProperties payrollProperties;

    // 급여 계산 메서드
    @CheckPermission(resource = "salary", action = "CREATE", scope = "COMPANY")
    public List<SalaryCalculationRes> calculateSalary(@AuthUser UUID memberPositionId, UUID companyId, YearMonth yearMonth) {
        // 산정 기간 계산
        SalaryPolicy salaryPolicy = salaryPolicyService.getLatestSalaryHistoryForCalculation(companyId);
        PayPeriodRes period =
                salaryPolicyService.calculatePeriodEndDate(salaryPolicy, yearMonth);
        LocalDate startDate = period.getStartDate();
        LocalDate endDate = period.getEndDate();

        // 급여 산정일 기준 기본급 조회
        List<SalaryHistory> salaryHistoryList = salaryHistoryService.getSalaryHistories(
                new SalaryHistoryListReq(companyId, yearMonth)
        );

        // 회원 정보 조회
        ApiResponse<List<MemberSalaryListRes>> salaryListResponse =
                memberClient.getSalaryList(memberPositionId, companyId);
        List<MemberSalaryListRes> salaryList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

        Map<UUID, MemberSalaryListRes> salaryMap = salaryList.stream()
                .collect(Collectors.toMap(MemberSalaryListRes::getMemberId, s -> s));

        // 급여 계산
        List<SalaryCalculationRes> result = new ArrayList<>();
        int workingDays = holidayService.getScheduledWorkingDays(startDate, endDate);

        Map<UUID, List<SalaryDetailRes>> allowanceMap = calculateAllowances(companyId, startDate, endDate);

        List<FixedAllowanceRes> fixedAllowanceList =  fixedAllowanceService.getFixedAllowanceList(memberPositionId,
                companyId);

        Map<UUID, List<FixedAllowanceRes>> fixedAllowanceMap = fixedAllowanceList.stream()
                .collect(Collectors.groupingBy(FixedAllowanceRes::getMemberId));

        for (SalaryHistory salaryHistory : salaryHistoryList) {

            BigInteger baseSalary = BigInteger.valueOf(salaryHistory.getBaseSalary());

            // 지급항목 계산
            List<SalaryDetailRes> allowanceList = allowanceMap.getOrDefault(salaryHistory.getMemberId(), new ArrayList<>());

            if (baseSalary.compareTo(BigInteger.ZERO) > 0) {
                allowanceList.add(
                        SalaryDetailRes.builder()
                                .salaryName("기본급")
                                .salaryType(SalaryType.ALLOWANCE.name())
                                .amount(baseSalary)
                                .build()
                );
            }

            // 고정항목 계산
            List<FixedAllowanceRes> fixedList = fixedAllowanceMap.getOrDefault(salaryHistory.getMemberId(), new ArrayList<>());

            // 공제항목 계산
            List<SalaryDetailRes> deductionList = calculateDeductions(
                    companyId, salaryHistory.getMemberId(), baseSalary, workingDays
            );

            // 총액 계산
            BigInteger totalAttendanceAllowance = allowanceList.stream()
                    .map(SalaryDetailRes::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            BigInteger totalFixedAllowance = fixedList.stream()
                    .map(allowance -> BigInteger.valueOf(allowance.getAmount()))
                    .reduce(BigInteger.ZERO, BigInteger::add);

            BigInteger totalAllowance = totalAttendanceAllowance.add(totalFixedAllowance);

            BigInteger totalDeduction = deductionList.stream()
                    .map(SalaryDetailRes::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            // 지급액 없으면 pass
            if (totalAllowance.equals(BigInteger.ZERO)) {
                continue;
            }

            BigInteger netPay = totalAllowance.subtract(totalDeduction);

            // 회원 정보 조회
            MemberSalaryListRes memberInfo = salaryMap.get(salaryHistory.getMemberId());
            String memberName = memberInfo != null ? memberInfo.getMemberName() : "";
            String department = memberInfo != null ? memberInfo.getOrganizationName() : "";
            String sabun = memberInfo != null ? memberInfo.getSabun() : "";

            SalaryCalculationRes res = SalaryCalculationRes.builder()
                    .salaryId(null)
                    .memberId(salaryHistory.getMemberId())
                    .sabun(sabun)
                    .memberName(memberName)
                    .department(department)
                    .workingDays(workingDays)
                    .periodStartDate(startDate)
                    .periodEndDate(endDate)
                    .paymentDate(endDate)
                    .allowanceList(allowanceList)
                    .deductionList(deductionList)
                    .fixedList(fixedList)
                    .totalAllowance(totalAllowance)
                    .totalDeduction(totalDeduction)
                    .netPay(netPay)
                    .build();

            result.add(res);
        }

        return result;
    }

    // 지급항목 계산
    private Map<UUID, List<SalaryDetailRes>> calculateAllowances(UUID companyId, LocalDate startDate, LocalDate endDate) {
        Map<UUID, List<SalaryDetailRes>> allowanceMap = new HashMap<>();

        List<PayrollItem> payrollItemList =
                payrollItemRepository.findByCompanyIdIsNullAndSalaryTypeAndIsTaxableAndCalculationCodeNot(
                        SalaryType.ALLOWANCE,
                        Bool.TRUE,
                        "BASE_SALARY");

        List<DailyAttendanceRes> dailyAttendanceResList = attendanceService.getMemberAttendance(companyId
                , startDate
                , endDate);

        SalaryHistoryListReq salaryHistoryListReq = new SalaryHistoryListReq(companyId
                , YearMonth.of(endDate.getYear(), endDate.getMonth()));

        List<SalaryHistory> salaryHistoryList = salaryHistoryService.getSalaryHistories(salaryHistoryListReq);

        Map<UUID, SalaryHistory> salaryHistoryMap = salaryHistoryList.stream()
                .collect(Collectors.toMap(SalaryHistory::getMemberId, sh -> sh));

        for (DailyAttendanceRes dailyAttendanceRes : dailyAttendanceResList) {
            UUID memberId = dailyAttendanceRes.getMemberId();

            SalaryHistory salaryHistory = salaryHistoryMap.get(memberId);

            if (salaryHistory == null) {
                log.warn("급여 계약 정보가 없는 회원입니다 (계산 제외): {}", memberId);
                continue;
            }

            BigInteger baseSalary = BigInteger.valueOf(salaryHistory.getBaseSalary());
            if (baseSalary.compareTo(BigInteger.ZERO) > 0) {
                SalaryDetailRes baseSalaryRes = SalaryDetailRes.builder()
                        .salaryName("기본급")
                        .salaryType(SalaryType.ALLOWANCE.name())
                        .amount(baseSalary)
                        .build();

                allowanceMap.computeIfAbsent(memberId, k -> new ArrayList<>()).add(baseSalaryRes);
            }

            // 통상 시급 계산
            BigDecimal hourlyWage = calculateHourlyWage(salaryHistory);
            BigDecimal minuteWage = hourlyWage.divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);

            for (PayrollItem payrollItem : payrollItemList) {

                String calcCode = payrollItem.getCalculationCode();
                BigDecimal amount = BigDecimal.ZERO;

                if ("OVERTIME_ALLOWANCE".equals(calcCode) && dailyAttendanceRes.getSumDaytimeOvertime() > 0) {
                    // 연장근로 (1.5배)
                    BigDecimal minutes = new BigDecimal(dailyAttendanceRes.getSumDaytimeOvertime());
                    amount = minutes.multiply(minuteWage).multiply(new BigDecimal("1.5"));

                } else if ("NIGHT_WORK_ALLOWANCE".equals(calcCode) && dailyAttendanceRes.getSumNightWork() > 0) {
                    // 야간근로 '가산' (0.5배)
                    BigDecimal minutes = new BigDecimal(dailyAttendanceRes.getSumNightWork());
                    amount = minutes.multiply(minuteWage).multiply(new BigDecimal("0.5"));

                } else if ("HOLIDAY_WORK_ALLOWANCE".equals(calcCode) && dailyAttendanceRes.getSumHolidayWork() > 0) {
                    // 휴일근로 (1.5배)
                    BigDecimal minutes = new BigDecimal(dailyAttendanceRes.getSumHolidayWork());
                    amount = minutes.multiply(minuteWage).multiply(new BigDecimal("1.5"));
                }

                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    SalaryDetailRes salaryDetailRes = SalaryDetailRes.builder()
                            .salaryName(payrollItem.getName())
                            .salaryType(SalaryType.ALLOWANCE.name())
                            .amount(amount.toBigInteger())
                            .build();
                    allowanceMap.computeIfAbsent(memberId, k -> new ArrayList<>()).add(salaryDetailRes);
                }

            }
        }

        return allowanceMap;

    }

    // 통상 시급 계산
    private BigDecimal calculateHourlyWage(SalaryHistory salaryHistory) {
        BigDecimal hourlyWage = BigDecimal.valueOf(salaryHistory.getCustomaryWage());
        PayType payType = salaryHistory.getPayType();

        BigDecimal MONTHLY_HOURS = payrollProperties.getConstants().getMonthlyHours();
        RoundingMode roundingMode = RoundingMode.HALF_UP;
        int scale = 2;

        return switch (payType) {
            case HOURLY -> hourlyWage.setScale(scale, roundingMode);
            case MONTHLY -> hourlyWage.divide(MONTHLY_HOURS, scale, roundingMode);
            case ANNUAL -> {
                BigDecimal monthly = hourlyWage.divide(new BigDecimal("12"), scale, roundingMode);
                yield monthly.divide(MONTHLY_HOURS, scale, roundingMode);
            }
            default -> throw new RuntimeException("알 수 없는 급여 계약 타입: " + payType);
        };
    }

    // 공제항목 계산
    private List<SalaryDetailRes> calculateDeductions(UUID companyId, UUID memberId, BigInteger baseSalary, int workingDays) {
        List<SalaryDetailRes> deductionList = new ArrayList<>();

        PayrollProperties.Rates rates = payrollProperties.getRates();
        BigDecimal baseSalaryDecimal = new BigDecimal(baseSalary);

        // 국민연금
        BigDecimal nationalPension = baseSalaryDecimal.multiply(rates.getNationalPension())
                .setScale(0, RoundingMode.DOWN);
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("국민연금")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(nationalPension.toBigInteger())
                .build());

        // 건강보험
        BigDecimal healthInsurance = baseSalaryDecimal.multiply(rates.getHealthInsurance())
                .setScale(0, RoundingMode.DOWN);
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("건강보험")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(healthInsurance.toBigInteger())
                .build());

        // 장기요양보험
        BigDecimal longTermCare = baseSalaryDecimal.multiply(rates.getLongTermCareInsurance())
                .setScale(0, RoundingMode.DOWN);
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("장기요양보험")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(longTermCare.toBigInteger())
                .build());

        // 고용보험
        BigDecimal employmentInsurance = baseSalaryDecimal.multiply(rates.getEmploymentInsurance())
                .setScale(0, RoundingMode.DOWN);
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("고용보험")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(employmentInsurance.toBigInteger())
                .build());

        // 근로소득세
        int dependentCount = 1;
        long incomeTaxLong = (long) incomeTaxService.lookupTaxTable(baseSalary.longValue(), dependentCount);
        BigDecimal incomeTax = BigDecimal.valueOf(incomeTaxLong);
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("근로소득세")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(incomeTax.toBigInteger())
                .build());

        // 지방소득세
        BigDecimal localIncomeTax = incomeTax.multiply(rates.getLocalIncomeTax())
                .setScale(0, RoundingMode.DOWN);
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("지방소득세")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(localIncomeTax.toBigInteger())
                .build());

        return deductionList;
    }
}
