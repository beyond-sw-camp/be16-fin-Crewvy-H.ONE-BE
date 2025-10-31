package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.workforce_service.attendance.dto.response.DailyAttendanceRes;
import com.crewvy.workforce_service.attendance.service.AttendanceService;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberSalaryListRes;
import com.crewvy.workforce_service.salary.constant.PayType;
import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.dto.request.*;
import com.crewvy.workforce_service.salary.dto.response.*;
import com.crewvy.workforce_service.salary.entity.*;
import com.crewvy.workforce_service.salary.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SalaryService {

    private final double NATIONAL_PENSION_RATE = 0.045;
    private final double HEALTH_INSURANCE_RATE = 0.03545;
    private final double LONG_TERM_CARE_INSURANCE_RATE = 0.1295;
    private final double EMPLOYMENT_INSURANCE_RATE = 0.009;
    private final double LOCAL_INCOME_TAX_RATE = 0.1;

    private final PayrollItemRepository payrollItemRepository;
    private final SalaryHistoryService salaryHistoryService;
    private final SalaryPolicyService salaryPolicyService;
    private final IncomeTaxService incomeTaxService;
    private final MemberClient memberClient;
    private final HolidayRepository holidayRepository;
    private final SalaryRepository salaryRepository;
    private final SalaryDetailRepository salaryDetailRepository;
    private final AttendanceService attendanceService;
    private final FixedAllowanceService fixedAllowanceService;

    // 급여 저장 메서드
    @Transactional
    public void saveSalary(UUID memberPositionId, UUID companyId, List<SalaryCreateReq> salaryCreateReqList) {
        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "CREATE", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        List<SalaryDetail> allDetailsToSave = new ArrayList<>();

        for (SalaryCreateReq salaryCreateReq : salaryCreateReqList) {

            Salary salaryToSave = salaryCreateReq.toEntity(companyId);
            Salary savedSalary = salaryRepository.save(salaryToSave);

            for (SalaryDetail salaryDetail : salaryCreateReq.getAllowanceList()) {
                salaryDetail.setSalary(savedSalary);
                salaryDetail.setSalaryType(SalaryType.ALLOWANCE);
                allDetailsToSave.add(salaryDetail);
            }

            for (SalaryDetail salaryDetail : salaryCreateReq.getDeductionList()) {
                salaryDetail.setSalary(savedSalary);
                salaryDetail.setSalaryType(SalaryType.DEDUCTION);
                allDetailsToSave.add(salaryDetail);
            }
        }

        if (!allDetailsToSave.isEmpty()) {
            salaryDetailRepository.saveAll(allDetailsToSave);
        }

    }

    // 급여 계산 메서드
    public List<SalaryCalculationRes> calculateSalary(UUID memberPositionId, SalaryCalculationReq request) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "CREATE", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        UUID companyId = request.getCompanyId();
        YearMonth yearMonth = request.getYearMonth();

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
        int workingDays = getScheduledWorkingDays(startDate, endDate);

        Map<UUID, List<SalaryDetailRes>> allowanceMap = calculateAllowances(companyId, startDate, endDate);

        List<FixedAllowanceRes> fixedAllowanceList =  fixedAllowanceService.getFixedAllowanceList(companyId);

        Map<UUID, List<FixedAllowanceRes>> fixedAllowanceMap = fixedAllowanceList.stream()
                .collect(Collectors.groupingBy(FixedAllowanceRes::getMemberId));

        for (SalaryHistory salaryHistory : salaryHistoryList) {

            BigInteger baseSalary = BigInteger.valueOf(salaryHistory.getBaseSalary());

            // 지급항목 계산
            List<SalaryDetailRes> allowanceList = allowanceMap.getOrDefault(salaryHistory.getMemberId(), new ArrayList<>());

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

        BigDecimal MONTHLY_HOURS = new BigDecimal("209");
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

        // 국민연금
        BigInteger nationalPension = baseSalary.multiply(BigInteger.valueOf((long)(NATIONAL_PENSION_RATE * 100)))
                .divide(BigInteger.valueOf(100));
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("국민연금")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(nationalPension)
                .build());

        // 건강보험
        BigInteger healthInsurance = baseSalary.multiply(BigInteger.valueOf((long)(HEALTH_INSURANCE_RATE * 100)))
                .divide(BigInteger.valueOf(100));
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("건강보험")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(healthInsurance)
                .build());

        // 장기요양보험
        BigInteger longTermCare = healthInsurance.multiply(BigInteger.valueOf((long)(LONG_TERM_CARE_INSURANCE_RATE * 100)))
                .divide(BigInteger.valueOf(100));
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("장기요양보험")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(longTermCare)
                .build());

        // 고용보험
        BigInteger employmentInsurance = baseSalary.multiply(BigInteger.valueOf((long)(EMPLOYMENT_INSURANCE_RATE * 100)))
                .divide(BigInteger.valueOf(100));
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("고용보험")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(employmentInsurance)
                .build());

        // 근로소득세
        int dependentCount = 1;
        long incomeTax = (long) incomeTaxService.lookupTaxTable(baseSalary.longValue(), dependentCount);
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("근로소득세")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(BigInteger.valueOf(incomeTax))
                .build());

        // 지방소득세
        BigInteger localIncomeTax = BigInteger.valueOf((long)(incomeTax * LOCAL_INCOME_TAX_RATE));
        deductionList.add(SalaryDetailRes.builder()
                .salaryName("지방소득세")
                .salaryType(SalaryType.DEDUCTION.name())
                .amount(localIncomeTax)
                .build());

        return deductionList;
    }

    // 소정 근로 일수 반환
    public int getScheduledWorkingDays(LocalDate startDate, LocalDate endDate) {

        int workingDays = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

            // 주말인지 확인
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            // 공휴일인지 확인
            boolean isHoliday = false;
            if (!isWeekend) {
                isHoliday = holidayRepository.existsBySolarDate(date);
            }

            // 둘 다 아니면 workingDays 카운트
            if (!isWeekend && !isHoliday) {
                workingDays++;
            }
        }

        return workingDays;
    }

    // 회사 전체 급여 조회
    @Transactional(readOnly = true)
    public List<SalaryCalculationRes> getSalaryListByCompany(UUID memberPositionId, UUID companyId) {
        // Salary 엔티티 조회
        List<Salary> salaryList = salaryRepository.findByCompanyIdOrderByPaymentDateDesc(companyId);

        // 회원 정보 조회
        ApiResponse<List<MemberSalaryListRes>> salaryListResponse =
                memberClient.getSalaryList(memberPositionId, companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

        Map<UUID, MemberSalaryListRes> memberMap = memberList.stream()
                .collect(Collectors.toMap(MemberSalaryListRes::getMemberId, m -> m));

        // SalaryPolicy 조회
        SalaryPolicy salaryPolicy = salaryPolicyService.getLatestSalaryHistoryForCalculation(companyId);

        List<SalaryCalculationRes> result = new ArrayList<>();
        for (Salary salary : salaryList) {
            // 회원 정보 조회
            MemberSalaryListRes memberInfo = memberMap.get(salary.getMemberId());
            String memberName = memberInfo != null ? memberInfo.getMemberName() : "";
            String department = memberInfo != null ? memberInfo.getOrganizationName() : "";
            String sabun = memberInfo != null ? memberInfo.getSabun() : "";

            // 산정 기간 계산
            LocalDate paymentDate = salary.getPaymentDate();
            java.time.YearMonth yearMonth = java.time.YearMonth.from(paymentDate);
            com.crewvy.workforce_service.salary.dto.response.PayPeriodRes period =
                    salaryPolicyService.calculatePeriodEndDate(salaryPolicy, yearMonth);
            int workingDays = getScheduledWorkingDays(period.getStartDate(), period.getEndDate());

            SalaryCalculationRes res = SalaryCalculationRes.fromEntity(
                    salary, memberName, department, sabun, workingDays,
                    period.getStartDate(), period.getEndDate()
            );
            result.add(res);
        }

        return result;
    }

    // 회원별 급여 조회
    @Transactional(readOnly = true)
    public List<SalaryCalculationRes> getSalaryListByMember(UUID memberPositionId, UUID companyId, UUID memberId) {
        // Salary 엔티티 조회
        List<Salary> salaryList = salaryRepository.findByCompanyIdAndMemberIdOrderByPaymentDateDesc(companyId, memberId);

        // 회원 정보 조회
        ApiResponse<List<MemberSalaryListRes>> salaryListResponse =
                memberClient.getSalaryList(memberPositionId, companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

        Map<UUID, MemberSalaryListRes> memberMap = memberList.stream()
                .collect(Collectors.toMap(MemberSalaryListRes::getMemberId, m -> m));

        SalaryPolicy salaryPolicy = salaryPolicyService.getLatestSalaryHistoryForCalculation(companyId);

        List<SalaryCalculationRes> result = new ArrayList<>();
        for (Salary salary : salaryList) {
            MemberSalaryListRes memberInfo = memberMap.get(salary.getMemberId());
            String memberName = memberInfo != null ? memberInfo.getMemberName() : "";
            String department = memberInfo != null ? memberInfo.getOrganizationName() : "";
            String sabun = memberInfo != null ? memberInfo.getSabun() : "";

            // 산정 기간 계산
            LocalDate paymentDate = salary.getPaymentDate();
            java.time.YearMonth yearMonth = java.time.YearMonth.from(paymentDate);
            PayPeriodRes period = salaryPolicyService.calculatePeriodEndDate(salaryPolicy, yearMonth);
            int workingDays = getScheduledWorkingDays(period.getStartDate(), period.getEndDate());

            SalaryCalculationRes res = SalaryCalculationRes.fromEntity(
                    salary, memberName, department, sabun, workingDays,
                    period.getStartDate(), period.getEndDate()
            );
            result.add(res);
        }

        return result;
    }

    // 급여 일괄 수정
    @Transactional
    public List<SalaryCalculationRes> updateSalaries(UUID memberPositionId, List<SalaryUpdateReq> updateRequests) {
        List<SalaryCalculationRes> result = new ArrayList<>();

        for (SalaryUpdateReq request : updateRequests) {
            // Salary 조회
            Salary salary = salaryRepository.findById(request.getSalaryId())
                    .orElseThrow(() -> new IllegalArgumentException("급여를 찾을 수 없습니다. ID: " + request.getSalaryId()));

            // Salary 기본 정보 수정
            salary.updateSalary(request.getTotalAllowance(),
                    request.getTotalDeduction(),
                    request.getNetPay(),
                    request.getPaymentDate());

            if (request.getDetailList() != null && !request.getDetailList().isEmpty()) {
                for (SalaryDetailUpdateReq detailReq : request.getDetailList()) {
                    if (detailReq.getDetailId() != null) {
                        // 기존 상세 항목 수정
                        SalaryDetail detail = salaryDetailRepository.findById(detailReq.getDetailId())
                                .orElseThrow(() -> new IllegalArgumentException("급여 상세 항목을 찾을 수 없습니다. ID: " + detailReq.getDetailId()));

                        salary.getSalaryDetailList().remove(detail);

                        SalaryDetail newDetail = SalaryDetail.builder()
                                .salary(salary)
                                .salaryType(SalaryType.valueOf(detailReq.getSalaryType()))
                                .salaryName(detailReq.getSalaryName())
                                .amount(detailReq.getAmount())
                                .build();
                        salary.getSalaryDetailList().add(newDetail);
                    }
                }
            }

            salary = salaryRepository.save(salary);

            UUID companyId = salary.getCompanyId();
            ApiResponse<List<MemberSalaryListRes>> salaryListResponse =
                    memberClient.getSalaryList(memberPositionId, companyId);
            List<MemberSalaryListRes> memberList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

            Map<UUID, MemberSalaryListRes> memberMap = memberList.stream()
                    .collect(Collectors.toMap(MemberSalaryListRes::getMemberId, m -> m));

            MemberSalaryListRes memberInfo = memberMap.get(salary.getMemberId());
            String memberName = memberInfo != null ? memberInfo.getMemberName() : "";
            String department = memberInfo != null ? memberInfo.getOrganizationName() : "";
            String sabun = memberInfo != null ? memberInfo.getSabun() : "";

            SalaryPolicy salaryPolicy = salaryPolicyService.getLatestSalaryHistoryForCalculation(companyId);
            LocalDate paymentDate = salary.getPaymentDate();
            YearMonth yearMonth = YearMonth.from(paymentDate);
            PayPeriodRes period = salaryPolicyService.calculatePeriodEndDate(salaryPolicy, yearMonth);
            int workingDays = getScheduledWorkingDays(period.getStartDate(), period.getEndDate());

            SalaryCalculationRes res = SalaryCalculationRes.fromEntity(
                    salary, memberName, department, sabun, workingDays,
                    period.getStartDate(), period.getEndDate()
            );
            result.add(res);
        }

        return result;
    }
}
