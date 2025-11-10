package com.crewvy.workforce_service.attendance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.attendance.dto.request.EventRequest;
import com.crewvy.workforce_service.attendance.dto.request.UpdateMemberBalanceRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId) {

        List<MyBalanceRes> response = attendanceService.getMyAllBalances(memberId, memberPositionId, companyId);
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
     * 팀원 근태 현황 조회 (날짜 범위 지정 가능, 페이징 지원)
     * @param startDate 시작 날짜 (optional, 기본값: 오늘)
     * @param endDate 종료 날짜 (optional, 기본값: 오늘)
     * @param pageable 페이징 정보 (page, size, sort)
     */
    @GetMapping("/team/status")
    public ResponseEntity<ApiResponse<Page<TeamMemberAttendanceRes>>> getTeamAttendanceStatus(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {

        Page<TeamMemberAttendanceRes> response = attendanceService.getTeamAttendanceStatus(
                memberId, memberPositionId, companyId, startDate, endDate, pageable);
        return new ResponseEntity<>(ApiResponse.success(response, "팀원 근태 현황 조회 완료"), HttpStatus.OK);
    }

    /**
     * 연차 현황 조회 (권한에 따라 조회 범위 자동 결정, 페이징 지원)
     * - COMPANY 권한: 전사 직원 연차 현황
     * - TEAM 권한: 본인 조직 및 하위 조직 직원 연차 현황
     * @param year 연도 (optional, 기본값: 현재 연도)
     * @param searchQuery 검색어 (이름, 부서)
     * @param policyTypeCode 정책 유형 코드 (PTC001 등)
     * @param yearsOfService 근속년수 필터 (<1, >=1, >=3, >=5, >=10)
     * @param pageable 페이징 정보 (page, size, sort)
     */
    @GetMapping("/leave-balance/status")
    public ResponseEntity<ApiResponse<Page<MemberBalanceSummaryRes>>> getLeaveBalanceStatus(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false) String policyTypeCode,
            @RequestParam(required = false) String yearsOfService,
            Pageable pageable) {

        // year가 null이면 현재 연도 사용
        Integer targetYear = year != null ? year : java.time.Year.now().getValue();

        Page<MemberBalanceSummaryRes> response = attendanceService.getLeaveBalanceStatus(
                memberId, memberPositionId, companyId, targetYear, searchQuery, policyTypeCode, yearsOfService, pageable);
        return new ResponseEntity<>(ApiResponse.success(response, "연차 현황 조회 완료"), HttpStatus.OK);
    }

    /**
     * 관리자가 회원의 연차 잔액을 직접 수정하는 API
     * @param balanceId 수정할 MemberBalance ID
     * @param request 수정 요청 데이터 (totalGranted, totalUsed)
     */
    @PutMapping("/leave-balance/{balanceId}")
    public ResponseEntity<ApiResponse<Void>> updateMemberBalance(
            @RequestHeader("X-User-UUID") UUID adminMemberId,
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID balanceId,
            @Valid @RequestBody UpdateMemberBalanceRequest request) {

        attendanceService.updateMemberBalance(balanceId, request, adminMemberId, companyId);
        return new ResponseEntity<>(ApiResponse.success(null, "연차 정보가 성공적으로 수정되었습니다."), HttpStatus.OK);
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

    /**
     * 근태 기록 수정 (관리자 전용)
     */
    @PutMapping("/daily/{dailyAttendanceId}")
    public ResponseEntity<ApiResponse<Void>> updateDailyAttendance(
            @PathVariable UUID dailyAttendanceId,
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestBody @Valid com.crewvy.workforce_service.attendance.dto.request.UpdateDailyAttendanceReq request) {

        attendanceService.updateDailyAttendance(dailyAttendanceId, memberPositionId, request);
        return new ResponseEntity<>(ApiResponse.success(null, "근태 기록이 수정되었습니다."), HttpStatus.OK);
    }

}