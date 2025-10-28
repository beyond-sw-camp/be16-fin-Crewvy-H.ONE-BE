package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.dto.request.SalaryCalculationReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.SalaryCalculationRes;
import com.crewvy.workforce_service.salary.service.SalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> getSalaryListByCompany(@RequestParam UUID companyId) {
        List<SalaryCalculationRes> response = salaryService.getSalaryListByCompany(companyId);
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
}
