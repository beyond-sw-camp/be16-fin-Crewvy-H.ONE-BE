package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.entity.Bool;
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

    @Transactional(readOnly = true)
    public List<PayrollItemRes> getPayrollItemList(UUID companyId) {
        List<PayrollItem> payrollItemList = payrollItemRepository.findByCompanyIdOrCompanyIdIsNull(companyId);
        return payrollItemList.stream()
                .map(PayrollItemRes::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PayrollItemRes> getPayrollItemsByType(UUID companyId, SalaryType salaryType) {
        List<PayrollItem> items = payrollItemRepository.findByCompanyIdAndSalaryTypeOrderByCreatedAtAsc(companyId, salaryType);
        return items.stream()
                .map(PayrollItemRes::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayrollItemRes getPayrollItem(UUID id) {
        PayrollItem item = payrollItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("급여 항목을 찾을 수 없습니다. ID: " + id));
        return PayrollItemRes.fromEntity(item);
    }

    public PayrollItemRes createPayrollItem(PayrollItemCreateReq req) {
        PayrollItem item = req.toEntity();
        PayrollItem savedItem = payrollItemRepository.save(item);
        return PayrollItemRes.fromEntity(savedItem);
    }

    public List<PayrollItemRes> updatePayrollItems(List<PayrollItemUpdateReq> requests) {
        return requests.stream()
                .map(this::updatePayrollItem)
                .collect(Collectors.toList());
    }

    private PayrollItemRes updatePayrollItem(PayrollItemUpdateReq req) {
        PayrollItem item = payrollItemRepository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("급여 항목을 찾을 수 없습니다. ID: " + req.getId()));
        
        item.updateItem(req.getSalaryType(), req.getName(), req.getIsActive(), req.getDescription());
        PayrollItem savedItem = payrollItemRepository.save(item);
        return PayrollItemRes.fromEntity(savedItem);
    }

    public void deletePayrollItems(List<UUID> ids) {
        List<PayrollItem> items = payrollItemRepository.findAllById(ids);
        if (items.size() != ids.size()) {
            throw new IllegalArgumentException("일부 급여 항목을 찾을 수 없습니다.");
        }
        payrollItemRepository.deleteAll(items);
    }
    
    // 고정 지급 수당 항목 조회
    public List<PayrollItemFixedRes> getFixedAllowanceHistory(UUID companyId) {
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
}
