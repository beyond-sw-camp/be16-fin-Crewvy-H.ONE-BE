package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.dto.request.PayrollItemCreateReq;
import com.crewvy.workforce_service.salary.dto.request.PayrollItemUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.PayrollItemRes;
import com.crewvy.workforce_service.salary.service.PayrollItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payrollItem")
@RequiredArgsConstructor
@Slf4j
public class PayrollItemController {

    private final PayrollItemService payrollItemService;

    // 급여 항목 목록 조회
    @GetMapping("/list")
    public ApiResponse<?> getPayrollItems(@RequestParam UUID companyId) {
        List<PayrollItemRes> response = payrollItemService.getPayrollItems(companyId);
        return new ApiResponse<>(true, response, "목록 조회 성공");
    }

    // 급여 항목 타입별 목록 조회
    @GetMapping("/list/type")
    public ApiResponse<List<PayrollItemRes>> getPayrollItemsByType(
            @RequestParam UUID companyId, 
            @RequestParam SalaryType salaryType) {
        List<PayrollItemRes> response = payrollItemService.getPayrollItemsByType(companyId, salaryType);
        return new ApiResponse<>(true, response, "타입별 목록 조회 성공");
    }

    // 급여 항목 상세 조회
    @GetMapping("/{id}")
    public ApiResponse<PayrollItemRes> getPayrollItem(@PathVariable UUID id) {
        PayrollItemRes response = payrollItemService.getPayrollItem(id);
        return new ApiResponse<>(true, response, "상세 조회 성공");
    }

    // 급여 항목 추가
    @PostMapping
    public ApiResponse<?> createPayrollItem(@RequestBody PayrollItemCreateReq request) {
        PayrollItemRes response = payrollItemService.createPayrollItem(request);
        return new ApiResponse<>(true, response, "항목 추가 성공");
    }

    // 급여 항목 수정
    @PutMapping
    public ApiResponse<List<PayrollItemRes>> updatePayrollItems(@RequestBody List<PayrollItemUpdateReq> requests) {
        List<PayrollItemRes> response = payrollItemService.updatePayrollItems(requests);
        return new ApiResponse<>(true, response, "항목 수정 성공");
    }

    // 급여 항목 삭제 
    @DeleteMapping
    public ApiResponse<?> deletePayrollItems(@RequestBody List<UUID> ids) {
        payrollItemService.deletePayrollItems(ids);
        return new ApiResponse<>(true, null, "항목 삭제 성공");
    }
}
