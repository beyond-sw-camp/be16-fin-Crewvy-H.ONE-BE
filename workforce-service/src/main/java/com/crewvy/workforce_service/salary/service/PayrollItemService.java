package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.entity.Bool;
import com.crewvy.common.exception.DuplicateResourceException;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.workforce_service.aop.AuthUser;
import com.crewvy.workforce_service.aop.CheckPermission;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.dto.request.PayrollItemCreateReq;
import com.crewvy.workforce_service.salary.dto.request.PayrollItemUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.PayrollItemFixedRes;
import com.crewvy.workforce_service.salary.dto.response.PayrollItemRes;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import com.crewvy.workforce_service.salary.repository.PayrollItemRepository;
import jakarta.persistence.EntityExistsException;
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
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<PayrollItemRes> getPayrollItemList(@AuthUser UUID memberPositionId, UUID companyId) {

        List<PayrollItem> payrollItemList = payrollItemRepository.findByCompanyIdOrCompanyIdIsNull(companyId);
        return payrollItemList.stream()
                .map(PayrollItemRes::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<PayrollItemRes> getPayrollItemsByType(@AuthUser UUID memberPositionId, UUID companyId, SalaryType salaryType) {

        List<PayrollItem> items = payrollItemRepository.findByCompanyIdAndSalaryType(companyId, salaryType);
        return items.stream()
                .map(PayrollItemRes::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public PayrollItemRes getPayrollItem(@AuthUser UUID memberPositionId, UUID id) {

        PayrollItem item = payrollItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("급여 항목을 찾을 수 없습니다. ID: " + id));
        return PayrollItemRes.fromEntity(item);
    }

    @CheckPermission(resource = "salary", action = "CREATE", scope = "COMPANY")
    public PayrollItemRes createPayrollItem(@AuthUser UUID memberPositionId, PayrollItemCreateReq req) {

        if (payrollItemRepository.existsByName(req.getName())) {
            throw new DuplicateResourceException("이미 동일한 이름의 급여 항목이 존재합니다.");
        }

        PayrollItem item = req.toEntity();
        PayrollItem savedItem = payrollItemRepository.save(item);
        return PayrollItemRes.fromEntity(savedItem);
    }

    @Transactional
    @CheckPermission(resource = "salary", action = "UPDATE", scope = "COMPANY")
    public List<PayrollItemRes> updatePayrollItems(@AuthUser UUID memberPositionId, List<PayrollItemUpdateReq> requests) {

        return requests.stream()
                .map(payrollItemUpdateReq
                        -> updatePayrollItem(memberPositionId, payrollItemUpdateReq))
                .collect(Collectors.toList());
    }

    private PayrollItemRes updatePayrollItem(@AuthUser UUID memberPositionId, PayrollItemUpdateReq req) {

        PayrollItem item = payrollItemRepository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("급여 항목을 찾을 수 없습니다. ID: " + req.getId()));
        
        item.updateItem(req.getSalaryType(), req.getName(), req.getIsActive(), req.getDescription());
        return PayrollItemRes.fromEntity(item);
    }

    @CheckPermission(resource = "salary", action = "DELETE", scope = "COMPANY")
    public void deletePayrollItems(@AuthUser UUID memberPositionId, List<UUID> ids) {

        List<PayrollItem> items = payrollItemRepository.findAllById(ids);
        if (items.size() != ids.size()) {
            throw new IllegalArgumentException("일부 급여 항목을 찾을 수 없습니다.");
        }
        payrollItemRepository.deleteAll(items);
    }
    
    // 고정 지급 수당 항목 조회
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<PayrollItemFixedRes> getFixedAllowanceHistory(@AuthUser UUID memberPositionId, UUID companyId) {

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
    @CheckPermission(resource = "salary", action = "READ", scope = "COMPANY")
    public List<PayrollItem> getDeduction(UUID memberPositionId, UUID companyId) {
        return payrollItemRepository
                .findApplicableForCompany(companyId, SalaryType.DEDUCTION);
    }
}
