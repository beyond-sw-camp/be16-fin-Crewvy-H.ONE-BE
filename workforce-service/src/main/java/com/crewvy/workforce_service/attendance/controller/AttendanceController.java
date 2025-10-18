package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.EventRequest;
import com.crewvy.workforce_service.attendance.dto.response.DailyAttendanceSummaryRes;
import com.crewvy.workforce_service.attendance.dto.response.MemberBalanceSummaryRes;
import com.crewvy.workforce_service.attendance.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<?>> recordEvent(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestBody @Valid EventRequest request,
            HttpServletRequest httpServletRequest) {

        String clientIp = getClientIp(httpServletRequest);

        Object response = attendanceService.recordEvent(memberId, memberPositionId, companyId, request, clientIp);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 기간별 전체 직원 일일 근태 조회 (급여 정산용)
     *
     * 현재 구현: COMPANY 레벨만 지원 (salary 권한 필요)
     * TODO: Phase 2 - targetMemberId 파라미터 추가하여 개인/팀장 조회 지원
     */
    @GetMapping("/summary/daily")
    public ResponseEntity<ApiResponse<List<DailyAttendanceSummaryRes>>> getDailyAttendanceSummary(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DailyAttendanceSummaryRes> response = attendanceService.getDailyAttendanceSummary(
                memberPositionId, companyId, startDate, endDate);
        return new ResponseEntity<>(ApiResponse.success(response, "일일 근태 데이터 조회 완료"), HttpStatus.OK);
    }

    /**
     * 연도별 전체 직원 잔여 일수 조회 (급여 정산용)
     *
     * 현재 구현: COMPANY 레벨만 지원 (salary 권한 필요)
     * TODO: Phase 2 - targetMemberId 파라미터 추가하여 개인 조회 지원
     */
    @GetMapping("/summary/balance")
    public ResponseEntity<ApiResponse<List<MemberBalanceSummaryRes>>> getMemberBalanceSummary(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam("year") Integer year) {
        List<MemberBalanceSummaryRes> response = attendanceService.getMemberBalanceSummary(
                memberPositionId, companyId, year);
        return new ResponseEntity<>(ApiResponse.success(response, "잔여 일수 조회 완료"), HttpStatus.OK);
    }
}