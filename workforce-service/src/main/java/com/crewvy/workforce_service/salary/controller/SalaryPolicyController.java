package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.dto.request.SalaryPolicyCreateReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryPolicyUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.SalaryPolicyRes;
import com.crewvy.workforce_service.salary.entity.SalaryPolicy;
import com.crewvy.workforce_service.salary.service.SalaryPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/salary-policy")
@RequiredArgsConstructor
@Slf4j
public class SalaryPolicyController {

    private final SalaryPolicyService salaryPolicyService;

    // 급여 정책 등록
    @PostMapping("/create")
    public ResponseEntity<?> createSalaryPolicy(@RequestBody SalaryPolicyCreateReq salaryPolicyCreateReq) {
        SalaryPolicy response = salaryPolicyService.createSalaryPolicy(salaryPolicyCreateReq);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "급여 정책 등록 성공")
        , HttpStatus.CREATED);
    }
    
    // 급여 정책 조회
    @GetMapping("/list")
    public ResponseEntity<?> getSalaryPolicy(@RequestHeader("X-User-CompanyId") UUID companyId) {
        SalaryPolicyRes response = salaryPolicyService.getSalaryPolicy(companyId);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "급여 정책 조회 성공")
                , HttpStatus.OK);
    }

    // 급여 정책 수정
    @PutMapping("/update")
    public ResponseEntity<?> updateSalaryPolicy(@RequestBody SalaryPolicyUpdateReq salaryPolicyUpdateReq) {
        salaryPolicyService.updateSalaryPolicy(salaryPolicyUpdateReq);
        return new ResponseEntity<>(new ApiResponse<>(true, null, "급여 정책 수정 성공")
                , HttpStatus.OK);
    }

    // 급여 지급일 계산
    @GetMapping("/payment-date")
    public ResponseEntity<?> getPaymentDate(@RequestHeader("X-User-CompanyId") UUID companyId,
                                            @RequestParam YearMonth yearMonth) {
        LocalDate paymentDate = salaryPolicyService.getPaymentDate(companyId, yearMonth);
        return new ResponseEntity<>(new ApiResponse<>(true, paymentDate, "급여 지급일 계산 성공")
                , HttpStatus.OK);
    }
}
