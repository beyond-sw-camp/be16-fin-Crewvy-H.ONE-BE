package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.EventRequest;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import com.crewvy.workforce_service.attendance.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestHeader("X-User-OrganizationId") UUID organizationId,
            @RequestBody @Valid EventRequest request,
            HttpServletRequest httpServletRequest) {

        String clientIp = getClientIp(httpServletRequest);

        Object response = attendanceService.recordEvent(memberId, memberPositionId, companyId, organizationId, request, clientIp);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    /**
     * 오늘의 내 출퇴근 현황 조회
     */
    @GetMapping("/my/today")
    public ResponseEntity<ApiResponse<DailyAttendance>> getMyTodayAttendance(
            @RequestHeader("X-User-UUID") UUID memberId) {

        DailyAttendance response = attendanceService.getMyTodayAttendance(memberId);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    /**
     * 월별 내 출퇴근 현황 조회
     */
    @GetMapping("/my/monthly")
    public ResponseEntity<ApiResponse<List<DailyAttendance>>> getMyMonthlyAttendance(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam int year,
            @RequestParam int month) {

        List<DailyAttendance> response = attendanceService.getMyMonthlyAttendance(memberId, companyId, year, month);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    /**
     * 내 연차 잔여 일수 조회
     */
    @GetMapping("/my/balance")
    public ResponseEntity<ApiResponse<MemberBalance>> getMyBalance(
            @RequestHeader("X-User-UUID") UUID memberId) {

        MemberBalance response = attendanceService.getMyBalance(memberId);
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

}