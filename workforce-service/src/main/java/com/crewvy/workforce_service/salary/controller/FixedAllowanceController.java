package com.crewvy.workforce_service.salary.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.salary.dto.request.FixedAllowanceCreateAllReq;
import com.crewvy.workforce_service.salary.dto.request.FixedAllowanceCreateReq;
import com.crewvy.workforce_service.salary.service.FixedAllowanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fixed-allowance")
@RequiredArgsConstructor
@Slf4j
public class FixedAllowanceController {

    private final FixedAllowanceService fixedAllowanceService;

    @PostMapping("/create")
    public ResponseEntity<?> createFixedAllowance(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                  @RequestBody FixedAllowanceCreateReq fixedAllowanceCreateReq) {
        Long id = fixedAllowanceService.saveFixedAllowance(memberPositionId, fixedAllowanceCreateReq);
        return new ResponseEntity<>(new ApiResponse<>(true, id, "고정 수당 저장 성공")
                , HttpStatus.CREATED);
    }

    @PutMapping("/update-all")
    public ResponseEntity<?> createAllFixedAllowance(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                     @RequestParam UUID companyId,
                                                     @RequestBody List<FixedAllowanceCreateAllReq> listReq) {
        fixedAllowanceService.saveAllFixedAllowance(memberPositionId, companyId, listReq);
        return new ResponseEntity<>(new ApiResponse<>(true, null, "고정 수당 일괄 저장 성공")
                , HttpStatus.CREATED);
    }
}
