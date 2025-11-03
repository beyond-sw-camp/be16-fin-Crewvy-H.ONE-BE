package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.workforce_service.aop.CheckPermission;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberSalaryListRes;
import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.dto.request.SalaryCreateReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryDetailUpdateReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.PayPeriodRes;
import com.crewvy.workforce_service.salary.dto.response.SalaryCalculationRes;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SalaryService {

    private final PayrollItemRepository payrollItemRepository;
    private final SalaryPolicyService salaryPolicyService;
    private final MemberClient memberClient;
    private final SalaryRepository salaryRepository;
    private final SalaryDetailRepository salaryDetailRepository;
    private final PayrollItemService payrollItemService;
    private final HolidayService holidayService;

    // 급여 저장 메서드
    @Transactional
    @CheckPermission(resource = "salary", action = "CREATE", scope = "COMPANY")
    public void saveSalary(UUID memberPositionId, UUID companyId, List<SalaryCreateReq> salaryCreateReqList) {
        // 권한 검증
//        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
//                "salary", "CREATE", "COMPANY");
//
//        if (Boolean.FALSE.equals(hasPermission.getData())) {
//            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
//        }

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

    // 급여 일괄 수정
    @Transactional
    @CheckPermission(resource = "salary", action = "UPDATE", scope = "COMPANY")
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
            int workingDays = holidayService.getScheduledWorkingDays(period.getStartDate(), period.getEndDate());

            SalaryCalculationRes res = SalaryCalculationRes.fromEntity(
                    salary, memberName, department, sabun, workingDays,
                    period.getStartDate(), period.getEndDate()
            );
            result.add(res);
        }

        return result;
    }
}
