package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.constant.SalaryType;
import com.crewvy.workforce_service.salary.dto.request.PayrollItemCreateReq;
import com.crewvy.workforce_service.salary.dto.request.PayrollItemUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.PayrollItemFixedRes;
import com.crewvy.workforce_service.salary.dto.response.PayrollItemRes;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import com.crewvy.workforce_service.salary.service.PayrollItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> getPayrollItems(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                             @RequestHeader("X-User-CompanyId") UUID companyId) {
        List<PayrollItemRes> response = payrollItemService.getPayrollItemList(memberPositionId, companyId);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "목록 조회 성공"), HttpStatus.OK);
    }

    // 급여 항목 타입별 목록 조회
    @GetMapping("/list/type")
    public ResponseEntity<?> getPayrollItemsByType(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                   @RequestHeader("X-User-CompanyId") UUID companyId,
                                                   @RequestParam SalaryType salaryType) {
        List<PayrollItemRes> response = payrollItemService.getPayrollItemsByType(memberPositionId,
                companyId,
                salaryType);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "타입별 목록 조회 성공")
                , HttpStatus.OK);
    }

    // 급여 항목 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getPayrollItem(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                            @PathVariable UUID id) {
        PayrollItemRes response = payrollItemService.getPayrollItem(memberPositionId, id);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "상세 조회 성공"), HttpStatus.OK);
    }

    // 급여 항목 추가
    @PostMapping
    public ResponseEntity<?> createPayrollItem(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                               @RequestBody PayrollItemCreateReq request) {
        PayrollItemRes response = payrollItemService.createPayrollItem(memberPositionId, request);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "항목 추가 성공"), HttpStatus.CREATED);
    }

    // 급여 항목 수정
    @PutMapping
    public ResponseEntity<?> updatePayrollItems(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                @RequestBody List<PayrollItemUpdateReq> requests) {
        List<PayrollItemRes> response = payrollItemService.updatePayrollItems(memberPositionId, requests);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "항목 수정 성공"), HttpStatus.OK);
    }

    // 급여 항목 삭제
    @DeleteMapping
    public ResponseEntity<?> deletePayrollItems(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                @RequestBody List<UUID> ids) {
        payrollItemService.deletePayrollItems(memberPositionId, ids);
        return new ResponseEntity<>(new ApiResponse<>(true, null, "항목 삭제 성공"), HttpStatus.OK);
    }

    // 고정 수당 항목 조회
    @GetMapping("/fixed-allowance")
    public ResponseEntity<?> deletePayrollItems(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                @RequestHeader("X-User-CompanyId") UUID companyId) {
        List<PayrollItemFixedRes> fixedAllowanceList = payrollItemService.getFixedAllowanceHistory(memberPositionId,
                companyId);
        return new ResponseEntity<>(
                new ApiResponse<>(true, fixedAllowanceList, "고정 수당 항목 조회 성공")
                , HttpStatus.OK);
    }

    // 공제 항목 조회
    @GetMapping("/deduction")
    public ResponseEntity<?> getDeduction(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @RequestHeader("X-User-CompanyId") UUID companyId) {
        List<PayrollItem> deductionList = payrollItemService.getDeduction(memberPositionId, companyId);
        List<String> deductionNames = deductionList.stream()
                .map(PayrollItem::getName)
                .toList();
        return new ResponseEntity<>(
                new ApiResponse<>(true, deductionNames, "공제 항목 조회 성공")
                , HttpStatus.OK);
    }
}
