package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.dto.request.SalaryInfoCreateReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryInfoUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.SalaryInfoRes;
import com.crewvy.workforce_service.salary.service.SalaryInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/salaryInfo")
@RequiredArgsConstructor
@Slf4j
public class SalaryInfoController {

    private final SalaryInfoService salaryInfoService;

    // 급여 기본 정보 전체 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getSalaryInfos(@RequestParam UUID companyId) {
        List<SalaryInfoRes> response = salaryInfoService.getSalaryInfoList(companyId);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "전체 목록 조회 성공"), HttpStatus.OK);
    }

    // 급여 기본 정보 저장
    @PostMapping("/save")
    public ResponseEntity<?> saveSalaryInfo(@RequestBody List<SalaryInfoCreateReq> requests) {
        List<SalaryInfoRes> response = salaryInfoService.saveSalaryInfoList(requests);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "항목 저장 성공"), HttpStatus.CREATED);
    }

    // 급여 기본 정보 수정
    @PutMapping("/update")
    public ResponseEntity<?> updateSalaryInfo(@RequestBody List<SalaryInfoUpdateReq> requests) {
        List<SalaryInfoRes> response = salaryInfoService.updateSalaryInfoList(requests);
        return new ResponseEntity<>(new ApiResponse<>(true, response, "항목 수정 성공"), HttpStatus.OK);
    }

    // 급여 기본 정보 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteSalaryInfo(@RequestBody List<UUID> uuidList) {
        salaryInfoService.deleteSalaryInfoList(uuidList);
        return new ResponseEntity<>(new ApiResponse<>(true, null, "항목 삭제 성공"), HttpStatus.OK);
    }
}
