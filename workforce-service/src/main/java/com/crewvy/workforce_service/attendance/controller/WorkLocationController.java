package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.WorkLocationCreateDto;
import com.crewvy.workforce_service.attendance.dto.request.WorkLocationUpdateDto;
import com.crewvy.workforce_service.attendance.dto.response.WorkLocationResponse;
import com.crewvy.workforce_service.attendance.service.WorkLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/work-locations")
@RequiredArgsConstructor
public class WorkLocationController {

    private final WorkLocationService workLocationService;

    /**
     * 근무지 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkLocationResponse>> createWorkLocation(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestBody @Valid WorkLocationCreateDto request) {

        WorkLocationResponse response = workLocationService.createWorkLocation(companyId, request);
        return new ResponseEntity<>(ApiResponse.success(response, "근무지가 생성되었습니다."), HttpStatus.CREATED);
    }

    /**
     * 근무지 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<WorkLocationResponse>>> getWorkLocations(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<WorkLocationResponse> response = workLocationService.getWorkLocations(companyId, pageable);
        return new ResponseEntity<>(ApiResponse.success(response, "근무지 목록 조회 성공"), HttpStatus.OK);
    }

    /**
     * 활성화된 근무지 목록 조회 (전체)
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<WorkLocationResponse>>> getActiveWorkLocations(
            @RequestHeader("X-User-CompanyId") UUID companyId) {

        List<WorkLocationResponse> response = workLocationService.getActiveWorkLocations(companyId);
        return new ResponseEntity<>(ApiResponse.success(response, "활성 근무지 목록 조회 성공"), HttpStatus.OK);
    }

    /**
     * 근무지 상세 조회
     */
    @GetMapping("/{workLocationId}")
    public ResponseEntity<ApiResponse<WorkLocationResponse>> getWorkLocationById(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID workLocationId) {

        WorkLocationResponse response = workLocationService.getWorkLocationById(workLocationId, companyId);
        return new ResponseEntity<>(ApiResponse.success(response, "근무지 상세 조회 성공"), HttpStatus.OK);
    }

    /**
     * 근무지 정보 수정
     */
    @PutMapping("/{workLocationId}")
    public ResponseEntity<ApiResponse<WorkLocationResponse>> updateWorkLocation(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID workLocationId,
            @RequestBody @Valid WorkLocationUpdateDto request) {

        WorkLocationResponse response = workLocationService.updateWorkLocation(workLocationId, companyId, request);
        return new ResponseEntity<>(ApiResponse.success(response, "근무지 정보가 수정되었습니다."), HttpStatus.OK);
    }

    /**
     * 근무지 활성/비활성 상태 토글
     */
    @PatchMapping("/{workLocationId}/toggle-active")
    public ResponseEntity<ApiResponse<WorkLocationResponse>> toggleActiveStatus(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID workLocationId) {

        WorkLocationResponse response = workLocationService.toggleActiveStatus(workLocationId, companyId);
        return new ResponseEntity<>(ApiResponse.success(response, "근무지 상태가 변경되었습니다."), HttpStatus.OK);
    }

    /**
     * 근무지 삭제
     */
    @DeleteMapping("/{workLocationId}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkLocation(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID workLocationId) {

        workLocationService.deleteWorkLocation(workLocationId, companyId);
        return new ResponseEntity<>(ApiResponse.success(null, "근무지가 삭제되었습니다."), HttpStatus.OK);
    }
}
