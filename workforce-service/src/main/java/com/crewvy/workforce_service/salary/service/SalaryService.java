package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.workforce_service.attendance.dto.response.DailyAttendanceRes;
import com.crewvy.workforce_service.attendance.service.AttendanceService;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.MemberSalaryListRes;
import com.crewvy.workforce_service.feignClient.dto.response.NameDto;
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
import java.util.function.Function;
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
    private final PayrollItemService payrollItemService;

    // 급여 저장 메서드
    @Transactional
    public void saveSalary(UUID memberPositionId, UUID companyId, List<SalaryCreateReq> salaryCreateReqList) {
        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "CREATE", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        List<UUID> memberIdList = salaryCreateReqList.stream()
                .map(SalaryCreateReq::getMemberId)
                .distinct().toList();

        YearMonth yearMonth = YearMonth.from(salaryCreateReqList.get(0).getPaymentDate());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        boolean isCompleted = salaryRepository.existsSalary(memberIdList, startDate,
                endDate, SalaryStatus.PAID);

        if (isCompleted) {
            throw new IllegalStateException("이미 '지급 완료'된 급여 내역이 포함되어 있어 새로 저장할 수 없습니다.");
        }
        List<Salary> pendingSalaryList = salaryRepository.findAllActiveSalaries(
                memberIdList, startDate, endDate, SalaryStatus.PENDING
        );

        Map<UUID, Salary> pendingSalaryMap = pendingSalaryList.stream()
                .collect(Collectors.toMap(Salary::getMemberId, salary -> salary));

        List<SalaryDetail> salaryDetailList = new ArrayList<>();
        List<Salary> salaryListToInsert = new ArrayList<>();

        for (SalaryCreateReq saveSalaryReq : salaryCreateReqList) {
            UUID memberId = saveSalaryReq.getMemberId();

            Salary saveSalary = saveSalaryReq.toEntity(companyId);

            Salary existsSalary = pendingSalaryMap.get(memberId);

            if (existsSalary != null) {
                if (areSalariesEqual(existsSalary, saveSalary)) {
                    continue;
                } else {
                    existsSalary.updateSalaryStatus(SalaryStatus.CANCELED);
                    salaryListToInsert.add(saveSalary);
                }
            } else {
                salaryListToInsert.add(saveSalary);
            }
        }

        List<Salary> savedSalaryList = salaryRepository.saveAll(salaryListToInsert);

        Map<UUID, SalaryCreateReq> salaryCreateReqMapSalaryCreateReq = salaryCreateReqList.stream()
                .collect(Collectors.toMap(SalaryCreateReq::getMemberId, req -> req));

        for (Salary savedSalary : savedSalaryList) {
            SalaryCreateReq req = salaryCreateReqMapSalaryCreateReq.get(savedSalary.getMemberId());

            for (SalaryDetail salaryDetail : req.getAllowanceList()) {
                salaryDetail.setSalary(savedSalary);
                salaryDetail.setSalaryType(SalaryType.ALLOWANCE);
                salaryDetailList.add(salaryDetail);
            }
            for (SalaryDetail salaryDetail : req.getDeductionList()) {
                salaryDetail.setSalary(savedSalary);
                salaryDetail.setSalaryType(SalaryType.DEDUCTION);
                salaryDetailList.add(salaryDetail);
            }
        }

        if (!salaryDetailList.isEmpty()) {
            salaryDetailRepository.saveAll(salaryDetailList);
        }

    }

    // 저장 시 변경 확인 메서드
    private boolean areSalariesEqual(Salary oldSalary, Salary newSalary) {
        return oldSalary.getTotalAllowance().equals(newSalary.getTotalAllowance())
                && oldSalary.getTotalDeduction().equals(newSalary.getTotalDeduction())
                && oldSalary.getNetPay().equals(newSalary.getNetPay());
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

        List<FixedAllowanceRes> fixedAllowanceList =  fixedAllowanceService.getFixedAllowanceList(memberPositionId,
                companyId);

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
    public List<SalaryStatusRes> getSalaryListByCompany(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        // 급여 지급일 조회
        LocalDate paymentDate = salaryPolicyService.getPaymentDate(companyId, yearMonth);

        // Salary 엔티티 조회
        List<Salary> salaryList = salaryRepository.findByCompanyIdAndSalaryStatusNotAndPaymentDate(companyId,
                SalaryStatus.CANCELED,
                paymentDate);

        // 회원 정보 조회
        ApiResponse<List<MemberSalaryListRes>> salaryListResponse =
                memberClient.getSalaryList(memberPositionId, companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

        Map<UUID, MemberSalaryListRes> memberMap = memberList.stream()
                .collect(Collectors.toMap(MemberSalaryListRes::getMemberId, m -> m));

        List<SalaryStatusRes> result = new ArrayList<>();
        for (Salary salary : salaryList) {
            // 회원 정보 조회
            MemberSalaryListRes memberInfo = memberMap.get(salary.getMemberId());
            String memberName = memberInfo != null ? memberInfo.getMemberName() : "";
            String department = memberInfo != null ? memberInfo.getOrganizationName() : "";
            String role = memberInfo != null ? memberInfo.getTitleName() : "";

            SalaryStatusRes res = SalaryStatusRes.fromEntity(
                    salary);

            res.setMemberName(memberName);
            res.setDepartment(department);
            res.setRole(role);

            result.add(res);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<SalaryOutputRes> getSalaryOutputByCompany(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {
        
        // 권한 조회
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        // 급여 지급일 조회
        LocalDate paymentDate = salaryPolicyService.getPaymentDate(companyId, yearMonth);

        // Salary 엔티티 조회
        List<Salary> salaryList = salaryRepository.findByCompanyIdAndSalaryStatusNotAndPaymentDate(companyId,
                SalaryStatus.CANCELED,
                paymentDate);

        // 회원 정보 조회
        ApiResponse<List<MemberSalaryListRes>> salaryListResponse =
                memberClient.getSalaryList(memberPositionId, companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

        Map<UUID, MemberSalaryListRes> memberMap = memberList.stream()
                .collect(Collectors.toMap(MemberSalaryListRes::getMemberId, m -> m));

        List<SalaryOutputRes> result = new ArrayList<>();
        for (Salary salary : salaryList) {

            // 회원 정보 조회
            MemberSalaryListRes memberInfo = memberMap.get(salary.getMemberId());
            String bank = memberInfo != null ? memberInfo.getBank() : "";
            String bankAccount = memberInfo != null ? memberInfo.getBankAccount() : "";
            String sabun = memberInfo != null ? memberInfo.getSabun() : "";
            String memberName = memberInfo != null ? memberInfo.getMemberName() : "";
            String department = memberInfo != null ? memberInfo.getOrganizationName() : "";

            SalaryOutputRes res = SalaryOutputRes.builder()
                    .bank(bank)
                    .bankAccount(bankAccount)
                    .sabun(sabun)
                    .memberName(memberName)
                    .department(department)
                    .netPay(salary.getNetPay())
                    .build();

            result.add(res);
        }
        return result;
    }

    // 회원별 급여 조회
    @Transactional(readOnly = true)
    public List<SalaryCalculationRes> getSalaryListByMember(UUID memberPositionId, UUID companyId, UUID memberId) {

        // 권한 조회
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "INDIVIDUAL");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        IdListReq idListReq = new IdListReq();
        idListReq.setUuidList(List.of(memberId));

        ApiResponse<List<NameDto>> nameResponse =
                memberClient.getNameList(memberPositionId, idListReq);

        if (nameResponse == null || nameResponse.getData() == null || nameResponse.getData().isEmpty()) {
            return new ArrayList<>();
        }
        NameDto memberInfo = nameResponse.getData().get(0);
        List<Salary> salaryList = salaryRepository.findByCompanyIdAndMemberIdOrderByPaymentDateDesc(companyId, memberId);

        List<SalaryCalculationRes> result = new ArrayList<>();
        for (Salary salary : salaryList) {

            SalaryPolicy salaryPolicy = salaryPolicyService.getLatestSalaryHistoryForCalculation(companyId);
            LocalDate paymentDate = salary.getPaymentDate();
            YearMonth yearMonth = YearMonth.from(paymentDate);
            PayPeriodRes period = salaryPolicyService.calculatePeriodEndDate(salaryPolicy, yearMonth);
            int workingDays = getScheduledWorkingDays(period.getStartDate(), period.getEndDate());

            SalaryCalculationRes res = SalaryCalculationRes.fromEntity(
                    salary, memberInfo.getName(), null, null,
                    workingDays, period.getStartDate(), period.getEndDate()
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

    // 월별 공제 내역
    @Transactional(readOnly = true)
    public List<PayrollDeductionRes> getDeductionList(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {

        // 권한 조회
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        // 급여 지급일 조회
        LocalDate paymentDate = salaryPolicyService.getPaymentDate(companyId, yearMonth);

        // 공제 항목 조회
        List<PayrollItem> payrollDeductionList = payrollItemService.getDeduction(memberPositionId, companyId);
        log.error(payrollDeductionList.toString());
        Set<String> deductionNames = payrollDeductionList.stream()
                .map(PayrollItem::getName)
                .collect(Collectors.toSet());


        // 회원 정보 조회
        ApiResponse<List<MemberSalaryListRes>> salaryListResponse =
                memberClient.getSalaryList(memberPositionId, companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

        List<MemberSalaryListRes> filteredMemberList = memberList.stream()
                .filter(member -> {
                    String sabun = member.getSabun();
                    return sabun != null && !sabun.isEmpty() && Character.isDigit(sabun.charAt(0));
                }).toList();

        if (filteredMemberList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Salary> salaryList = salaryRepository.findByCompanyIdAndSalaryStatusNotAndPaymentDate(companyId,
                SalaryStatus.CANCELED,
                paymentDate);

        List<UUID> salaryIdList = salaryList.stream().map(Salary::getId).toList();

        List<SalaryDetail> allDeductionDetails = new ArrayList<>();
        if (!salaryIdList.isEmpty()) {
            allDeductionDetails = salaryDetailRepository
                    .findBySalaryIdInAndSalaryNameIn(salaryIdList, deductionNames);

        }

        Map<UUID, Salary> salaryByMemberId = salaryList.stream()
                .collect(Collectors.toMap(Salary::getMemberId, Function.identity()));

        Map<UUID, List<SalaryDetail>> detailsBySalaryId = allDeductionDetails.stream()
                .collect(Collectors.groupingBy(detail -> detail.getSalary().getId()));


        List<PayrollDeductionRes> responseList = new ArrayList<>();

        for (MemberSalaryListRes member : filteredMemberList) {
            PayrollDeductionRes payrollDeductionRes = PayrollDeductionRes.builder()
                    .memberId(member.getMemberId())
                    .memberName(member.getMemberName())
                    .department(member.getOrganizationName())
                    .period(yearMonth)
                    .build();

            Map<String, BigInteger> deductionAmountsMap = new HashMap<>();
            BigInteger totalDeduction = BigInteger.ZERO;

            Salary salary = salaryByMemberId.get(member.getMemberId());

            if (salary != null) {
                totalDeduction = salary.getTotalDeduction();
                payrollDeductionRes.setStatus("완료");

                if (totalDeduction != null && totalDeduction.compareTo(BigInteger.ZERO) > 0) {

                    List<SalaryDetail> memberDetails = detailsBySalaryId.getOrDefault(salary.getId(), new ArrayList<>());

                    for (SalaryDetail detail : memberDetails) {
                        deductionAmountsMap.put(detail.getSalaryName(), detail.getAmount());
                    }

                    for (String name : deductionNames) {
                        deductionAmountsMap.putIfAbsent(name, BigInteger.ZERO);
                    }
                }

            } else {
                payrollDeductionRes.setStatus("미완료");
            }

            payrollDeductionRes.setTotalDeductions(totalDeduction);
            payrollDeductionRes.setDeductionMap(deductionAmountsMap);
            responseList.add(payrollDeductionRes);
        }
        return responseList;
    }

    // 항목별 조회
    @Transactional(readOnly = true)
    public PayrollItemSummaryRes getPayrollItemSummary(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {

        // 권한 조회
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        // 회원 정보 조회
        ApiResponse<List<MemberSalaryListRes>> salaryListResponse = memberClient.getSalaryList(memberPositionId, companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse.getData();
        List<MemberSalaryListRes> filteredMemberList = memberList.stream()
                .filter(member -> {
                    String sabun = member.getSabun();
                    return sabun != null && !sabun.isEmpty() && Character.isDigit(sabun.charAt(0));
                }).toList();

        // 급여 지급일 조회
        LocalDate paymentDate = salaryPolicyService.getPaymentDate(companyId, yearMonth);

        List<Salary> salaryList = salaryRepository.findByCompanyIdAndSalaryStatusNotAndPaymentDate(companyId,
                SalaryStatus.CANCELED,
                paymentDate);
        List<UUID> salaryIdList = salaryList.stream().map(Salary::getId).toList();

        Map<String, BigInteger> totalAmountsMap = new HashMap<>();
        if (!salaryIdList.isEmpty()) {
            totalAmountsMap = salaryDetailRepository.sumAmountsBySalaryName(salaryIdList)
                    .stream()
                    .collect(Collectors.toMap(ItemTotalRes::getName, ItemTotalRes::getTotalAmount));
        }

        List<PayrollItem> allItems = payrollItemRepository.findApplicableForCompany(companyId, SalaryType.DEDUCTION);
        allItems.addAll(payrollItemRepository.findApplicableForCompany(companyId, SalaryType.ALLOWANCE));

        PayrollItemSummaryRes response = new PayrollItemSummaryRes();
        List<ItemTotalRes> paymentList = new ArrayList<>();
        List<ItemTotalRes> deductionList = new ArrayList<>();

        for (PayrollItem item : allItems) {
            BigInteger totalAmount = totalAmountsMap.getOrDefault(item.getName(), BigInteger.ZERO);

            if (item.getSalaryType() == SalaryType.ALLOWANCE) {
                paymentList.add(new ItemTotalRes(item.getName(), totalAmount));
            } else {
                deductionList.add(new ItemTotalRes(item.getName(), totalAmount));
            }
        }

        response.setPaymentItems(paymentList);
        response.setDeductionItems(deductionList);
        return response;
    }

    // 항목별 조회 모달
    @Transactional(readOnly = true)
    public List<PayrollItemDetailRes> getPayrollItemDetails(UUID memberPositionId, UUID companyId, String name, YearMonth yearMonth) {

        // 권한 조회
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        // 급여 지급일 조회
        LocalDate paymentDate = salaryPolicyService.getPaymentDate(companyId, yearMonth);

        ApiResponse<List<MemberSalaryListRes>> salaryListResponse =
                memberClient.getSalaryList(memberPositionId, companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse.getData();
        List<MemberSalaryListRes> filteredMemberList = memberList.stream()
                .filter(member -> {
                    String sabun = member.getSabun();
                    return sabun != null && !sabun.isEmpty() && Character.isDigit(sabun.charAt(0));
                }).toList();

        Map<UUID, MemberSalaryListRes> memberMap = filteredMemberList.stream()
                .collect(Collectors.toMap(MemberSalaryListRes::getMemberId, Function.identity()));

        List<UUID> memberIdList = new ArrayList<>(memberMap.keySet());

        List<Salary> salaryList = salaryRepository.findByMemberIdInAndPaymentDate(memberIdList, paymentDate);
        List<UUID> salaryIds = salaryList.stream().map(Salary::getId).toList();

        List<PayrollItemDetailRes> responseList = new ArrayList<>();
        if (salaryIds.isEmpty()) {
            return responseList;
        }

        List<SalaryDetail> details = salaryDetailRepository
                .findBySalaryIdInAndSalaryName(salaryIds, name);

        for (SalaryDetail detail : details) {
            UUID memberId = detail.getSalary().getMemberId();
            MemberSalaryListRes memberInfo = memberMap.get(memberId);

            if (memberInfo != null) {
                responseList.add(PayrollItemDetailRes.builder()
                        .memberId(memberId)
                        .department(memberInfo.getOrganizationName())
                        .role(memberInfo.getTitleName())
                        .sabun(memberInfo.getSabun())
                        .memberName(memberInfo.getMemberName())
                        .amount(detail.getAmount())
                        .build());
            }
        }

        return responseList;
    }

    // 급여 명세서 출력
    public List<PayrollStatementRes> getSalaryStatement(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {

        // 권한 조회
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        // 급여 지급일 조회
        LocalDate paymentDate = salaryPolicyService.getPaymentDate(companyId, yearMonth);

        ApiResponse<List<MemberSalaryListRes>> salaryListResponse =
                memberClient.getSalaryList(memberPositionId, companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse.getData();
        List<MemberSalaryListRes> filteredMemberList = memberList.stream()
                .filter(member -> {
                    String sabun = member.getSabun();
                    return sabun != null && !sabun.isEmpty() && Character.isDigit(sabun.charAt(0));
                }).toList();

        Map<UUID, MemberSalaryListRes> memberMap = filteredMemberList.stream()
                .collect(Collectors.toMap(MemberSalaryListRes::getMemberId, Function.identity()));
        List<UUID> memberIdList = new ArrayList<>(memberMap.keySet());
        
        // 급여 조회
        List<Salary> salaryList = salaryRepository.findByMemberIdInAndPaymentDate(memberIdList, paymentDate);

        Map<UUID, Salary> salaryByMemberId = salaryList.stream()
                .collect(Collectors.toMap(Salary::getMemberId, Function.identity()));

        final Set<String> INSURANCE_NAMES = Set.of("국민연금", "건강보험", "장기요양보험", "고용보험");
        final String BASE_PAY_NAME = "기본급";
        final String INCOME_TAX_NAME = "근로소득세";

        Set<String> detailItemNames = new HashSet<>(INSURANCE_NAMES);
        detailItemNames.add(BASE_PAY_NAME);
        detailItemNames.add(INCOME_TAX_NAME);

        List<UUID> salaryIds = salaryList.stream().map(Salary::getId).toList();

        List<SalaryDetail> allDetails = new ArrayList<>();
        if (!salaryIds.isEmpty()) {
            allDetails = salaryDetailRepository
                    .findBySalaryIdInAndSalaryNameIn(salaryIds, detailItemNames);
        }

        Map<UUID, List<SalaryDetail>> detailsBySalaryId = allDetails.stream()
                .collect(Collectors.groupingBy(detail -> detail.getSalary().getId()));


        List<PayrollStatementRes> responseList = new ArrayList<>();
        for (MemberSalaryListRes member : filteredMemberList) {

            Salary salary = salaryByMemberId.get(member.getMemberId());

            if (salary == null) {
                responseList.add(PayrollStatementRes.builder()
                        .memberId(member.getMemberId())
                        .memberName(member.getMemberName())
                        .salaryPeriod(yearMonth)
                        .basePay(BigInteger.ZERO)
                        .allowance(BigInteger.ZERO)
                        .totalPayment(BigInteger.ZERO)
                        .incomeTax(BigInteger.ZERO)
                        .fourInsurances(BigInteger.ZERO)
                        .netPay(BigInteger.ZERO)
                        .status("미완료")
                        .build());
                continue;
            }

            List<SalaryDetail> memberDetails = detailsBySalaryId.getOrDefault(salary.getId(), new ArrayList<>());

            BigInteger basePay = getAmountByName(memberDetails, BASE_PAY_NAME);
            BigInteger incomeTax = getAmountByName(memberDetails, INCOME_TAX_NAME);

            BigInteger fourInsurances = BigInteger.ZERO;
            for (String insName : INSURANCE_NAMES) {
                fourInsurances = fourInsurances.add(getAmountByName(memberDetails, insName));
            }

            BigInteger totalPayment = salary.getTotalAllowance() != null ? salary.getTotalAllowance() : BigInteger.ZERO;
            BigInteger allowance = totalPayment.subtract(basePay);


            responseList.add(PayrollStatementRes.builder()
                    .memberId(member.getMemberId())
                    .memberName(member.getMemberName())
                    .salaryPeriod(yearMonth)
                    .basePay(basePay)
                    .allowance(allowance)
                    .incomeTax(incomeTax)
                    .fourInsurances(fourInsurances)
                    .totalPayment(totalPayment)
                    .netPay(salary.getNetPay())
                    .status("완료")
                    .build());
        }
        return responseList;
    }

    private BigInteger getAmountByName(List<SalaryDetail> details, String itemName) {
        return details.stream()
                .filter(detail -> detail.getSalaryName().equals(itemName))
                .map(SalaryDetail::getAmount)
                .findFirst()
                .orElse(BigInteger.ZERO);
    }
}
