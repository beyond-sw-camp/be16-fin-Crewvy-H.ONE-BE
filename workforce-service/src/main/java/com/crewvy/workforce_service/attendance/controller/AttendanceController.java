package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.EventRequest;
import com.crewvy.workforce_service.attendance.dto.response.AssignedPolicyRes;
import com.crewvy.workforce_service.attendance.dto.response.MemberBalanceSummaryRes;
import com.crewvy.workforce_service.attendance.dto.response.MyBalanceRes;
import com.crewvy.workforce_service.attendance.dto.response.TeamMemberAttendanceRes;
import com.crewvy.workforce_service.attendance.dto.response.TodayAttendanceStatusResponse;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import com.crewvy.workforce_service.attendance.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
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
            @RequestBody @Valid EventRequest eventRequest,
            HttpServletRequest httpServletRequest) {

        String clientIp = getClientIp(httpServletRequest);

        Object response = attendanceService.recordEvent(memberId, memberPositionId, companyId, organizationId, eventRequest, clientIp);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.OK);
    }

    /**
     * 오늘의 내 출퇴근 현황 조회
     */
    @GetMapping("/my/today")
    public ResponseEntity<ApiResponse<TodayAttendanceStatusResponse>> getMyTodayAttendance(
            @RequestHeader("X-User-UUID") UUID memberId) {

        TodayAttendanceStatusResponse response = attendanceService.getMyTodayAttendance(memberId);
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

    /**
     * 내 모든 휴가 정책 잔액 조회 (연차, 병가, 육아휴직 등)
     */
    @GetMapping("/my/balances")
    public ResponseEntity<ApiResponse<List<MyBalanceRes>>> getMyAllBalances(
            @RequestHeader("X-User-UUID") UUID memberId) {

        List<MyBalanceRes> response = attendanceService.getMyAllBalances(memberId);
        return new ResponseEntity<>(ApiResponse.success(response, "휴가 정책 잔액 조회 완료"), HttpStatus.OK);
    }

    /**
     * 내게 할당된 모든 정책 조회 (휴가 신청 시 사용)
     */
    @GetMapping("/my/assigned-policies")
    public ResponseEntity<ApiResponse<List<AssignedPolicyRes>>> getMyAssignedPolicies(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId) {

        List<AssignedPolicyRes> response = attendanceService.getMyAssignedPolicies(memberId, memberPositionId, companyId);
        return new ResponseEntity<>(ApiResponse.success(response, "할당된 정책 조회 완료"), HttpStatus.OK);
    }

    /**
     * 팀원 근태 현황 조회 (오늘 날짜 기준)
     */
    @GetMapping("/team/status")
    public ResponseEntity<ApiResponse<List<TeamMemberAttendanceRes>>> getTeamAttendanceStatus(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId) {

        List<TeamMemberAttendanceRes> response = attendanceService.getTeamAttendanceStatus(memberId, memberPositionId, companyId);
        return new ResponseEntity<>(ApiResponse.success(response, "팀원 근태 현황 조회 완료"), HttpStatus.OK);
    }

    /**
     * 연차 현황 조회 (권한에 따라 조회 범위 자동 결정)
     * - COMPANY 권한: 전사 직원 연차 현황
     * - TEAM 권한: 본인 조직 및 하위 조직 직원 연차 현황
     */
    @GetMapping("/leave-balance/status")
    public ResponseEntity<ApiResponse<List<MemberBalanceSummaryRes>>> getLeaveBalanceStatus(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam(required = false) Integer year) {

        // year가 null이면 현재 연도 사용
        Integer targetYear = year != null ? year : java.time.Year.now().getValue();

        List<MemberBalanceSummaryRes> response = attendanceService.getLeaveBalanceStatus(memberId, memberPositionId, companyId, targetYear);
        return new ResponseEntity<>(ApiResponse.success(response, "연차 현황 조회 완료"), HttpStatus.OK);
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
        System.out.println("네트워크 정보 : " + ip);
        return ip;
    }

}