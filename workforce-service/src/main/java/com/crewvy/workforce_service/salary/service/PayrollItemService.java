package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.entity.Bool;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.dto.request.PayrollItemCreateReq;
import com.crewvy.workforce_service.salary.dto.request.PayrollItemUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.PayrollItemFixedRes;
import com.crewvy.workforce_service.salary.dto.response.PayrollItemRes;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import com.crewvy.workforce_service.salary.repository.PayrollItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class PayrollItemService {

    private final PayrollItemRepository payrollItemRepository;
    private final MemberClient memberClient;

    @Transactional(readOnly = true)
    public List<PayrollItemRes> getPayrollItemList(UUID memberPositionId, UUID companyId) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        List<PayrollItem> payrollItemList = payrollItemRepository.findByCompanyIdOrCompanyIdIsNull(companyId);
        return payrollItemList.stream()
                .map(PayrollItemRes::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayrollItemRes> getPayrollItemsByType(UUID memberPositionId, UUID companyId, SalaryType salaryType) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        List<PayrollItem> items = payrollItemRepository.findByCompanyIdAndSalaryType(companyId, salaryType);
        return items.stream()
                .map(PayrollItemRes::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayrollItemRes getPayrollItem(UUID memberPositionId, UUID id) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        PayrollItem item = payrollItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("급여 항목을 찾을 수 없습니다. ID: " + id));
        return PayrollItemRes.fromEntity(item);
    }

    public PayrollItemRes createPayrollItem(UUID memberPositionId, PayrollItemCreateReq req) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "CREATE", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        PayrollItem item = req.toEntity();
        PayrollItem savedItem = payrollItemRepository.save(item);
        return PayrollItemRes.fromEntity(savedItem);
    }

    public List<PayrollItemRes> updatePayrollItems(UUID memberPositionId, List<PayrollItemUpdateReq> requests) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "UPDATE", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        return requests.stream()
                .map(payrollItemUpdateReq
                        -> updatePayrollItem(memberPositionId, payrollItemUpdateReq))
                .collect(Collectors.toList());
    }

    private PayrollItemRes updatePayrollItem(UUID memberPositionId, PayrollItemUpdateReq req) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "UPDATE", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        PayrollItem item = payrollItemRepository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("급여 항목을 찾을 수 없습니다. ID: " + req.getId()));
        
        item.updateItem(req.getSalaryType(), req.getName(), req.getIsActive(), req.getDescription());
        PayrollItem savedItem = payrollItemRepository.save(item);
        return PayrollItemRes.fromEntity(savedItem);
    }

    public void deletePayrollItems(UUID memberPositionId, List<UUID> ids) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "DELETE", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        List<PayrollItem> items = payrollItemRepository.findAllById(ids);
        if (items.size() != ids.size()) {
            throw new IllegalArgumentException("일부 급여 항목을 찾을 수 없습니다.");
        }
        payrollItemRepository.deleteAll(items);
    }
    
    // 고정 지급 수당 항목 조회
    public List<PayrollItemFixedRes> getFixedAllowanceHistory(UUID memberPositionId, UUID companyId) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        List<PayrollItem> fixedItemResList = payrollItemRepository
                .findByCompanyIdAndSalaryTypeAndCalculationCodeIsNullAndIsTaxable(
                        companyId,
                        SalaryType.ALLOWANCE,
                        Bool.FALSE
                );

        return fixedItemResList.stream()
                .map(PayrollItemFixedRes::fromEntity)
                .toList();
    }

    // 고정 지급 수당 항목 조회
    public List<PayrollItem> getDeduction(UUID memberPositionId, UUID companyId) {

        // 권한 검증
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        return payrollItemRepository
                .findApplicableForCompany(companyId, SalaryType.DEDUCTION);
    }
}
