package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.LeaveRequestCreateDto;
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
            @RequestBody @Valid LeaveRequestCreateDto request) {
        LeaveRequestResponse response = requestService.createLeaveRequest(
                memberId, memberPositionId, companyId, organizationId, request);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.CREATED);
    }

    /**
     * 내 휴가 신청 목록 조회 (페이징)
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<LeaveRequestResponse>>> getMyRequests(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<LeaveRequestResponse> response = requestService.getMyRequests(memberId, pageable);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    /**
     * 휴가 신청 상세 조회
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> getRequestById(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID requestId) {

        LeaveRequestResponse response = requestService.getRequestById(requestId, memberId);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    /**
     * 휴가 신청 취소
     */
    @DeleteMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID requestId) {

        requestService.cancelRequest(requestId, memberId);
        return new ResponseEntity<>(ApiResponse.success(null), HttpStatus.OK);
    }
}
