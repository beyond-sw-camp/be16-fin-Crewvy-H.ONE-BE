package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.dto.request.SalaryHistoryCreateReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryHistoryListReq;
import com.crewvy.workforce_service.salary.dto.response.SalaryHistoryRes;
import com.crewvy.workforce_service.salary.entity.SalaryHistory;
import com.crewvy.workforce_service.salary.service.SalaryHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/salary-history")
@RequiredArgsConstructor
@Slf4j
public class SalaryHistoryController {

    private final SalaryHistoryService salaryHistoryService;

    @PutMapping("/save")
    public ResponseEntity<?> insertSalaryHistory(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                 @RequestHeader("X-User-CompanyId") UUID companyId,
                                                 @RequestBody List<SalaryHistoryCreateReq> reqList) {
        salaryHistoryService.updateSalaryHistory(memberPositionId, companyId, reqList);
        return new ResponseEntity<>(new ApiResponse<>(true, null, "급여 변경 등록 성공")
                , HttpStatus.CREATED);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getSalaryHistoryList(@RequestHeader("X-User-UUID") UUID memberId) {
        List<SalaryHistoryRes> response = salaryHistoryService.getSalaryHistoryListByMemberId(memberId);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "급여 이력 조회 성공")
                , HttpStatus.OK);
    }

    // 산정 종료일 기반 기본급 반환
    @GetMapping("/payroll-list")
    public ResponseEntity<?> getSalaryHistoryListForPayrollCalculation(
            @RequestBody SalaryHistoryListReq salaryHistoryListReq) {
        List<SalaryHistory> response = salaryHistoryService.getSalaryHistories(salaryHistoryListReq);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "급여 이력 조회 성공")
                , HttpStatus.OK);
    }


}
