package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.workforce_service.aop.CheckPermission;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.MemberSalaryListRes;
import com.crewvy.workforce_service.feignClient.dto.response.NameDto;
import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.dto.response.*;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import com.crewvy.workforce_service.salary.entity.Salary;
import com.crewvy.workforce_service.salary.entity.SalaryDetail;
import com.crewvy.workforce_service.salary.entity.SalaryPolicy;
import com.crewvy.workforce_service.salary.repository.PayrollItemRepository;
import com.crewvy.workforce_service.salary.repository.SalaryDetailRepository;
import com.crewvy.workforce_service.salary.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SalaryQueryService {

    private final MemberClient memberClient;
    private final SalaryPolicyService salaryPolicyService;
    private final SalaryRepository salaryRepository;
    private final HolidayService holidayService;
    private final SalaryDetailRepository salaryDetailRepository;
    private final PayrollItemService payrollItemService;
    private final PayrollItemRepository payrollItemRepository;

    // 회사 전체 급여 조회
    @Transactional(readOnly = true)
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<SalaryStatusRes> getSalaryListByCompany(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {
        
        // 권한 조회
//        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
//                "salary", "READ", "COMPANY");
//
//        if (Boolean.FALSE.equals(hasPermission.getData())) {
//            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
//        }

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
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<SalaryOutputRes> getSalaryOutputByCompany(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {

        // 권한 조회
//        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
//                "salary", "READ", "COMPANY");
//
//        if (Boolean.FALSE.equals(hasPermission.getData())) {
//            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
//        }

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
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<SalaryCalculationRes> getSalaryListByMember(UUID memberPositionId, UUID companyId, UUID memberId) {

        // 권한 조회
//        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
//                "salary", "READ", "INDIVIDUAL");
//
//        if (Boolean.FALSE.equals(hasPermission.getData())) {
//            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
//        }

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
            int workingDays = holidayService.getScheduledWorkingDays(period.getStartDate(), period.getEndDate());

            SalaryCalculationRes res = SalaryCalculationRes.fromEntity(
                    salary, memberInfo.getName(), null, null,
                    workingDays, period.getStartDate(), period.getEndDate()
            );
            result.add(res);
        }

        return result;
    }

    // 월별 공제 내역
    @Transactional(readOnly = true)
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<PayrollDeductionRes> getDeductionList(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {

        // 권한 조회
//        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
//                "salary", "READ", "COMPANY");
//
//        if (Boolean.FALSE.equals(hasPermission.getData())) {
//            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
//        }

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
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public PayrollItemSummaryRes getPayrollItemSummary(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {

        // 권한 조회
//        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
//                "salary", "READ", "COMPANY");
//
//        if (Boolean.FALSE.equals(hasPermission.getData())) {
//            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
//        }

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
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<PayrollItemDetailRes> getPayrollItemDetails(UUID memberPositionId, UUID companyId, String name, YearMonth yearMonth) {

        // 권한 조회
//        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
//                "salary", "READ", "COMPANY");
//
//        if (Boolean.FALSE.equals(hasPermission.getData())) {
//            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
//        }

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
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<PayrollStatementRes> getSalaryStatement(UUID memberPositionId, UUID companyId, YearMonth yearMonth) {

        // 권한 조회
//        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
//                "salary", "READ", "COMPANY");
//
//        if (Boolean.FALSE.equals(hasPermission.getData())) {
//            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
//        }

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
