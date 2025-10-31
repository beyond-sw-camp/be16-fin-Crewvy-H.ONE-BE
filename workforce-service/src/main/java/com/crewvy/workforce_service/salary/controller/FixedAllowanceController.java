package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.dto.request.FixedAllowanceCreateReq;
import com.crewvy.workforce_service.salary.service.FixedAllowanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fixed-allowance")
@RequiredArgsConstructor
@Slf4j
public class FixedAllowanceController {

    private final FixedAllowanceService fixedAllowanceService;

    @PostMapping("/create")
    public ResponseEntity<?> createFixedAllowance(@RequestBody FixedAllowanceCreateReq fixedAllowanceCreateReq) {
        Long id = fixedAllowanceService.saveFixedAllowance(fixedAllowanceCreateReq);
        return new ResponseEntity<>(new ApiResponse<>(true, id, "고정 수당 저장 성공")
                , HttpStatus.CREATED);
    }
}
