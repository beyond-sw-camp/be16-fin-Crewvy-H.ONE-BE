package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.dto.response.SalaryConfigRes;
import com.crewvy.workforce_service.salary.service.SalaryConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/salary-config")
@RequiredArgsConstructor
public class SalaryConfigController {

    private final SalaryConfigService salaryConfigService;

    // 급여 계약 정보 조회
    @GetMapping("/list")
    public ResponseEntity<?> getSalaryConfigList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                 @RequestParam UUID companyId) {
        List<SalaryConfigRes> salaryConfigResList = salaryConfigService.getSalaryConfigRes(memberPositionId,
                companyId);
        return new ResponseEntity<>(
                new ApiResponse<>(true, salaryConfigResList, "급여 계약 정보 조회 성공")
                , HttpStatus.OK);
    }
}
