package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.dto.request.SalaryCalculationReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryCreateReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.*;
import com.crewvy.workforce_service.salary.service.SalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/salary")
@RequiredArgsConstructor
@Slf4j
public class SalaryController {

    private final SalaryService salaryService;

    // 급여 계산
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateSalary(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                             @RequestBody SalaryCalculationReq request) {
        List<SalaryCalculationRes> response = salaryService.calculateSalary(memberPositionId, request);
        return new ResponseEntity<>(
            new ApiResponse<>(true, response, "급여 계산 성공"), 
            HttpStatus.OK
        );
    }

    // 회사 전체 급여 조회
    @GetMapping("/list")
    public ResponseEntity<?> getSalaryListByCompany(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                    @RequestParam UUID companyId,
                                                    @RequestParam YearMonth yearMonth) {
        List<SalaryStatusRes> response = salaryService.getSalaryListByCompany(memberPositionId, companyId, yearMonth);
        return new ResponseEntity<>(
            new ApiResponse<>(true, response, "급여 목록 조회 성공"),
            HttpStatus.OK
        );
    }

    // 회사 급여 이체 명세서
    @GetMapping("/output")
    public ResponseEntity<?> getSalaryOutputByCompany(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                    @RequestParam UUID companyId,
                                                    @RequestParam YearMonth yearMonth) {
        List<SalaryOutputRes> response = salaryService.getSalaryOutputByCompany(memberPositionId, companyId, yearMonth);
        return new ResponseEntity<>(
                new ApiResponse<>(true, response, "급여 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    // 회사 월별 공제 항목 조회
    @GetMapping("/deduction")
    public ResponseEntity<?> getDeductionList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                              @RequestParam UUID companyId,
                                              @RequestParam YearMonth yearMonth) {
        List<PayrollDeductionRes> response = salaryService.getDeductionList(memberPositionId, companyId, yearMonth);
        return new ResponseEntity<>(
                new ApiResponse<>(true, response, "급여 목록 조회 성공"),
                HttpStatus.OK
        );
    }


    // 회원별 급여 조회
    @GetMapping("/member")
    public ResponseEntity<?> getSalaryListByMember(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestParam UUID companyId,
            @RequestParam UUID memberId) {
        List<SalaryCalculationRes> response = salaryService.getSalaryListByMember(memberPositionId, companyId, memberId);
        return new ResponseEntity<>(
            new ApiResponse<>(true, response, "급여 목록 조회 성공"),
            HttpStatus.OK
        );
    }

    // 급여 일괄 수정
    @PutMapping("/update")
    public ResponseEntity<?> updateSalaries(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                            @RequestBody List<SalaryUpdateReq> requests) {
        List<SalaryCalculationRes> response = salaryService.updateSalaries(memberPositionId, requests);
        return new ResponseEntity<>(
            new ApiResponse<>(true, response, "급여 수정 성공"),
            HttpStatus.OK
        );
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveSalary(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                       @RequestParam UUID companyId,
                                       @RequestBody List<SalaryCreateReq> salaryCreateReqList) {
        salaryService.saveSalary(memberPositionId, companyId, salaryCreateReqList);
        return new ResponseEntity<>(
                new ApiResponse<>(true, null, "급여 저장 성공"),
                HttpStatus.OK
        );
    }
    
    @GetMapping("/summary")
    public ResponseEntity<?> getPayrollItemSummary(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                        @RequestParam UUID companyId,
                                        @RequestParam YearMonth yearMonth) {
        PayrollItemSummaryRes response = salaryService.getPayrollItemSummary(memberPositionId, companyId, yearMonth);
        return new ResponseEntity<>(
                new ApiResponse<>(true, response, "항목별 조회 성공"),
                HttpStatus.OK
        );
    }

    @GetMapping("/summary-details")
    public ResponseEntity<?> getPayrollItemDetails(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                   @RequestParam UUID companyId,
                                                   @RequestParam String name,
                                                   @RequestParam YearMonth yearMonth) {
        List<PayrollItemDetailRes> response = salaryService.getPayrollItemDetails(memberPositionId, companyId, name, yearMonth);
        return new ResponseEntity<>(
                new ApiResponse<>(true, response, "항목별 상세 조회 성공"),
                HttpStatus.OK
        );
    }

    @GetMapping("/statement")
    public ResponseEntity<?> getSalaryStatement(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                   @RequestParam UUID companyId,
                                                   @RequestParam YearMonth yearMonth) {
        List<PayrollStatementRes> response = salaryService.getSalaryStatement(memberPositionId, companyId, yearMonth);
        return new ResponseEntity<>(
                new ApiResponse<>(true, response, "항목별 상세 조회 성공"),
                HttpStatus.OK
        );
    }
}
