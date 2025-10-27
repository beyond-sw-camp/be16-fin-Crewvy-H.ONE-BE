package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.DeviceRequestCreateDto;
import com.crewvy.workforce_service.attendance.dto.request.LeaveRequestCreateDto;
import com.crewvy.workforce_service.attendance.dto.response.DeviceRequestResponse;
import com.crewvy.workforce_service.attendance.dto.response.LeaveRequestResponse;
import com.crewvy.workforce_service.attendance.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    /**
     * 휴가 신청 생성
     */
    @PostMapping("/leave")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> createLeaveRequest(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-OrganizationId") UUID organizationId,
            @RequestBody @Valid LeaveRequestCreateDto createDto) {
        LeaveRequestResponse response = requestService.createLeaveRequest(
                memberId, memberPositionId, companyId, organizationId, createDto);
        return new ResponseEntity<>(ApiResponse.success(response, "휴가 신청이 완료되었습니다."), HttpStatus.CREATED);
    }

    /**
     * 내 휴가 신청 목록 조회 (페이징)
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<LeaveRequestResponse>>> getMyRequests(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<LeaveRequestResponse> response = requestService.getMyRequests(memberId, pageable);
        return new ResponseEntity<>(ApiResponse.success(response, "휴가 신청 목록 조회 성공"), HttpStatus.OK);
    }

    /**
     * 휴가 신청 상세 조회
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> getRequestById(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID requestId) {

        LeaveRequestResponse response = requestService.getRequestById(requestId, memberId);
        return new ResponseEntity<>(ApiResponse.success(response, "휴가 신청 상세 조회 성공"), HttpStatus.OK);
    }

    /**
     * 휴가 신청 취소
     */
    @DeleteMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID requestId) {

        requestService.cancelRequest(requestId, memberId);
        return new ResponseEntity<>(ApiResponse.success(null, "휴가 신청이 취소되었습니다."), HttpStatus.OK);
    }

    // ==================== 디바이스 관리 ====================

    /**
     * 디바이스 등록 신청 (사용자)
     */
    @PostMapping("/devices/register")
    public ResponseEntity<ApiResponse<DeviceRequestResponse>> registerDevice(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestBody @Valid DeviceRequestCreateDto createDto) {

        DeviceRequestResponse response = requestService.registerDevice(memberId, createDto);
        return new ResponseEntity<>(ApiResponse.success(response, "디바이스 등록 신청이 완료되었습니다."), HttpStatus.CREATED);
    }

    /**
     * 내 디바이스 등록 신청 목록 조회 (사용자)
     */
    @GetMapping("/devices/my")
    public ResponseEntity<ApiResponse<Page<DeviceRequestResponse>>> getMyDeviceRequests(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<DeviceRequestResponse> response = requestService.getMyDeviceRequests(memberId, pageable);
        return new ResponseEntity<>(ApiResponse.success(response, "디바이스 목록 조회 성공"), HttpStatus.OK);
    }

    /**
     * 승인 대기 중인 디바이스 등록 신청 목록 (관리자)
     */
    @GetMapping("/devices/pending")
    public ResponseEntity<ApiResponse<Page<DeviceRequestResponse>>> getPendingDeviceRequests(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // TODO: 권한 체크 (관리자만 접근 가능)
        Page<DeviceRequestResponse> response = requestService.getPendingDeviceRequests(pageable);
        return new ResponseEntity<>(ApiResponse.success(response, "승인 대기 중인 디바이스 목록 조회 성공"), HttpStatus.OK);
    }

    /**
     * 디바이스 승인 (관리자)
     */
    @PostMapping("/devices/{requestId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveDeviceRequest(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @PathVariable UUID requestId) {

        // TODO: 권한 체크 (관리자만 접근 가능)
        requestService.approveDeviceRequest(requestId);
        return new ResponseEntity<>(ApiResponse.success(null, "디바이스가 승인되었습니다."), HttpStatus.OK);
    }

    /**
     * 디바이스 반려 (관리자)
     */
    @PostMapping("/devices/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectDeviceRequest(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @PathVariable UUID requestId) {

        // TODO: 권한 체크 (관리자만 접근 가능)
        requestService.rejectDeviceRequest(requestId);
        return new ResponseEntity<>(ApiResponse.success(null, "디바이스가 반려되었습니다."), HttpStatus.OK);
    }
}
