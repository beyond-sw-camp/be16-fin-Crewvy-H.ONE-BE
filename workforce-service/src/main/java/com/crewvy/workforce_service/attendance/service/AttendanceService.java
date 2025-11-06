package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.*;
import com.crewvy.workforce_service.attendance.constant.*;
import com.crewvy.workforce_service.attendance.dto.request.EventRequest;
import com.crewvy.workforce_service.attendance.dto.response.*;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.*;
import com.crewvy.workforce_service.attendance.repository.*;
import com.crewvy.workforce_service.attendance.util.DistanceCalculator;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.MemberDto;
import com.crewvy.workforce_service.feignClient.dto.response.MemberPositionListRes;
import com.crewvy.workforce_service.feignClient.dto.response.OrganizationNodeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final RequestRepository requestRepository;
    private final PolicyRepository policyRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final MemberClient memberClient;
    private final PolicyAssignmentService policyAssignmentService;
    private final MemberBalanceRepository memberBalanceRepository;
    private final WorkLocationRepository workLocationRepository;
    private final CompanyHolidayRepository companyHolidayRepository;

    // 분리된 서비스들
    private final AttendanceValidator attendanceValidator;
    private final AttendanceCalculator attendanceCalculator;

    // 로컬 개발 환경 설정
    @Value("${attendance.dev-mode.enabled:false}")
    private boolean devModeEnabled;

    @Value("${attendance.dev-mode.skip-location-auth:false}")
    private boolean skipLocationAuth;

    public ApiResponse<?> recordEvent(UUID memberId, UUID memberPositionId, UUID companyId, UUID organizationId, EventRequest request, String clientIp) {
//        checkPermissionOrThrow(memberPositionId, "attendance", "CREATE", "INDIVIDUAL", "근태를 기록할 권한이 없습니다.");

        // 인증/검증이 필요한 이벤트 그룹
        List<EventType> validationRequiredEvents = List.of(EventType.CLOCK_IN, EventType.CLOCK_OUT);

        if (validationRequiredEvents.contains(request.getEventType())) {
            validate(memberId, memberPositionId, companyId, request.getDeviceType(), request.getLatitude(), request.getLongitude(), clientIp, request.getWifiSsid());
        }

        switch (request.getEventType()) {
            case CLOCK_IN:
                ClockInResponse clockInResponse = clockIn(memberId, memberPositionId, companyId, request);
                return ApiResponse.success(clockInResponse, "출근 등록 완료.");
            case CLOCK_OUT:
                ClockOutResponse clockOutResponse = clockOut(memberId, memberPositionId, companyId, request);
                return ApiResponse.success(clockOutResponse, "퇴근 등록 완료.");
            case GO_OUT:
                GoOutResponse goOutResponse = goOut(memberId, memberPositionId, companyId, request);
                return ApiResponse.success(goOutResponse, "외출 등록 완료.");
            case COME_BACK:
                ComeBackResponse comeBackResponse = comeBack(memberId, memberPositionId, companyId, request);
                return ApiResponse.success(comeBackResponse, "복귀 등록 완료.");
            case BREAK_START:
                BreakStartResponse breakStartResponse = breakStart(memberId, memberPositionId, companyId, request);
                return ApiResponse.success(breakStartResponse, "휴게 시작 등록 완료.");
            case BREAK_END:
                BreakEndResponse breakEndResponse = breakEnd(memberId, memberPositionId, companyId, request);
                return ApiResponse.success(breakEndResponse, "휴게 종료 등록 완료.");
            default:
                throw new BusinessException("지원하지 않는 이벤트 타입입니다.");
        }
    }

    private void checkPermissionOrThrow(UUID memberPositionId, String resource, String action, String range, String errorMessage) {
        ApiResponse<Boolean> response = memberClient.checkPermission(memberPositionId, resource, action, range);
        if (response == null || !Boolean.TRUE.equals(response.getData())) {
            throw new PermissionDeniedException(errorMessage);
        }
    }

    /**
     * 권한 체크 (예외 발생 없이 boolean 반환)
     */
    private boolean hasPermission(UUID memberPositionId, String resource, String action, String range) {
        try {
            ApiResponse<Boolean> response = memberClient.checkPermission(memberPositionId, resource, action, range);
            return response != null && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.debug("Permission check failed for {}/{}/{}: {}", resource, action, range, e.getMessage());
            return false;
        }
    }

    private ClockInResponse clockIn(UUID memberId, UUID memberPositionId, UUID companyId, EventRequest request) {
        LocalDate today = LocalDate.now();
        LocalDateTime clockInTime = LocalDateTime.now();

        // 1. 오늘 승인된 휴가/휴직이 있는지 확인 (종일 휴가만 차단, 반차/시차는 허용)
        // BUSINESS_TRIP은 제외 (출장은 근무로 간주)
        java.util.List<PolicyTypeCode> fullDayBlockingTypes = java.util.List.of(
                PolicyTypeCode.ANNUAL_LEAVE,
                PolicyTypeCode.MATERNITY_LEAVE,
                PolicyTypeCode.PATERNITY_LEAVE,
                PolicyTypeCode.CHILDCARE_LEAVE,
                PolicyTypeCode.FAMILY_CARE_LEAVE,
                PolicyTypeCode.MENSTRUAL_LEAVE
        );
        boolean hasFullDayLeave = requestRepository.hasApprovedFullDayLeaveOnDate(
                memberId,
                today.atStartOfDay(),
                today.atTime(23, 59, 59),
                RequestStatus.APPROVED,
                fullDayBlockingTypes
        );

        if (hasFullDayLeave) {
            throw new BusinessException("오늘은 휴가/휴직으로 승인된 날짜입니다. 출근할 수 없습니다.");
        }

        // 2. 기존 DailyAttendance 조회 (반차/시차 승인되어 있을 수 있음)
        DailyAttendance existingAttendance = dailyAttendanceRepository
                .findByMemberIdAndAttendanceDate(memberId, today)
                .orElse(null);

        // 3. 기본근무 정책 조회 (WorkTimeRule, LatenessRule 포함)
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);

        // 정책이 할당되지 않은 경우 출근 불가
        if (standardWorkPolicy == null) {
            throw new BusinessException("근무 정책이 할당되지 않았습니다. 관리자에게 문의하세요.");
        }

        // 출근 가능 시간 범위 검증
        attendanceValidator.validateClockInTimeRange(clockInTime, standardWorkPolicy);

        DailyAttendance dailyAttendance;

        if (existingAttendance != null) {
            // 케이스 1: 오전 반차가 승인되어 있는 경우
            if (existingAttendance.getStatus() == AttendanceStatus.HALF_DAY_AM
                    && existingAttendance.getFirstClockIn() == null) {

                // 오전 반차 출근 시간 검증 (점심 종료 시간 + 지각 허용 시간)
                attendanceValidator.validateHalfDayAMClockIn(existingAttendance, standardWorkPolicy, clockInTime);

                // 기존 DailyAttendance에 출근 시간 업데이트
                dailyAttendance = existingAttendance;
                dailyAttendance.updateFirstClockIn(clockInTime);

                // 지각 검증 스킵 (오전 반차는 별도 기준 적용)
            }
            // 케이스 2: 오후 반차가 승인되어 있는 경우
            else if (existingAttendance.getStatus() == AttendanceStatus.HALF_DAY_PM
                    && existingAttendance.getFirstClockIn() == null) {

                // 오후 반차는 정상 출근 시간 적용
                dailyAttendance = existingAttendance;
                dailyAttendance.updateFirstClockIn(clockInTime);

                // 지각 검증 수행 (정규 출근 시간 기준)
                attendanceValidator.checkLateness(dailyAttendance, standardWorkPolicy);
            }
            // 케이스 3: 이미 출근 처리됨
            else {
                throw new DuplicateResourceException("이미 출근 처리되었습니다.");
            }
        } else {
            // 정상 출근 (반차/시차 없음)
            attendanceValidator.validateWorkingHoursLimit(today, standardWorkPolicy, clockInTime, null);

            try {
                dailyAttendance = createDailyAttendance(memberId, companyId, today, clockInTime);
            } catch (DataIntegrityViolationException e) {
                throw new DuplicateResourceException("이미 출근 처리되었습니다.");
            }

            // 지각 여부 판별
            attendanceValidator.checkLateness(dailyAttendance, standardWorkPolicy);
        }

        AttendanceLog newLog = createAttendanceLog(memberId, clockInTime, EventType.CLOCK_IN, request.getLatitude(), request.getLongitude());

        // 월별 지각 횟수 조회
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        int monthlyLatenessCount = dailyAttendanceRepository.countMonthlyLateness(memberId, monthStart, monthEnd);

        // 월별 허용 횟수
        Integer monthlyAllowedCount = null;
        if (standardWorkPolicy.getRuleDetails() != null
            && standardWorkPolicy.getRuleDetails().getLatenessRule() != null) {
            monthlyAllowedCount = standardWorkPolicy.getRuleDetails().getLatenessRule().getMonthlyAllowedCount();
        }

        return ClockInResponse.builder()
                .attendanceLogId(newLog.getId())
                .eventTime(newLog.getEventTime())
                .lateMinutes(dailyAttendance.getLateMinutes())
                .isLate(dailyAttendance.getIsLate())
                .monthlyLatenessCount(monthlyLatenessCount)
                .monthlyAllowedCount(monthlyAllowedCount)
                .build();
    }

    private ClockOutResponse clockOut(UUID memberId, UUID memberPositionId, UUID companyId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);

        EventType lastEvent = getLastEventTypeToday(memberId);
        // validateStateTransition(lastEvent, EventType.CLOCK_OUT); // 퇴근은 여러 번 가능하므로 상태 전환 검증 제거

        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);

        // 퇴근 중복 허용 (항상 허용 - 마지막 퇴근 시간이 최종 기록됨)

        LocalDateTime clockOutTime = LocalDateTime.now();

        // 퇴근 가능 시간 범위 검증
        attendanceValidator.validateClockOutTimeRange(clockOutTime, standardWorkPolicy);

        // 오후 반차 퇴근 시간 검증 (점심 시작 시간 이후 퇴근 가능)
        if (dailyAttendance.getStatus() == AttendanceStatus.HALF_DAY_PM) {
            attendanceValidator.validateHalfDayPMClockOut(dailyAttendance, standardWorkPolicy, clockOutTime);
            // 조퇴 검증 스킵 (오후 반차는 별도 기준)
        } else {
            // 정상 퇴근 시간 검증
            attendanceValidator.validateClockOutTime(dailyAttendance, standardWorkPolicy, clockOutTime);
        }

        AttendanceLog newLog = createAttendanceLog(memberId, clockOutTime, EventType.CLOCK_OUT, request.getLatitude(), request.getLongitude());
        Integer standardWorkMinutes = attendanceCalculator.getStandardWorkMinutes(standardWorkPolicy);

        // 반차/시차인 경우 근무시간 조정
        Integer requiredWorkMinutes = attendanceCalculator.calculateRequiredWorkMinutes(dailyAttendance, standardWorkMinutes);

        // 휴일 여부 확인 (주말 또는 CompanyHoliday)
        boolean isHoliday = attendanceCalculator.isHoliday(companyId, today);

        // AUTO 모드일 경우 자동으로 휴게 시간 계산
        attendanceCalculator.autoCalculateBreakTime(dailyAttendance, standardWorkPolicy, clockOutTime);
        dailyAttendance.updateClockOut(clockOutTime, requiredWorkMinutes, isHoliday);

        // 조퇴 검증 (오후 반차가 아닌 경우만)
        if (dailyAttendance.getStatus() != AttendanceStatus.HALF_DAY_PM) {
            attendanceValidator.checkEarlyLeave(dailyAttendance, standardWorkPolicy);
        }

        // 법정 최소 휴게 시간 검증 (근로기준법 제54조)
        attendanceValidator.validateMandatoryBreakTime(dailyAttendance, standardWorkPolicy);

        // 월별 조퇴 횟수 조회
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        int monthlyEarlyLeaveCount = dailyAttendanceRepository.countMonthlyEarlyLeave(memberId, monthStart, monthEnd);

        // 월별 허용 횟수
        Integer monthlyAllowedCount = null;
        if (standardWorkPolicy.getRuleDetails() != null
            && standardWorkPolicy.getRuleDetails().getLatenessRule() != null) {
            monthlyAllowedCount = standardWorkPolicy.getRuleDetails().getLatenessRule().getMonthlyAllowedCount();
        }

        return ClockOutResponse.builder()
                .attendanceLogId(newLog.getId())
                .eventTime(newLog.getEventTime())
                .totalWorkedMinutes(dailyAttendance.getWorkedMinutes())
                .totalOvertimeMinutes(dailyAttendance.getOvertimeMinutes())
                .earlyLeaveMinutes(dailyAttendance.getEarlyLeaveMinutes())
                .isEarlyLeave(dailyAttendance.getIsEarlyLeave())
                .monthlyEarlyLeaveCount(monthlyEarlyLeaveCount)
                .monthlyAllowedCount(monthlyAllowedCount)
                .build();
    }

    private GoOutResponse goOut(UUID memberId, UUID memberPositionId, UUID companyId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);
        EventType lastEvent = getLastEventTypeToday(memberId);
        validateStateTransition(lastEvent, EventType.GO_OUT);

        LocalDateTime goOutTime = LocalDateTime.now();
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), standardWorkPolicy, goOutTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, goOutTime, EventType.GO_OUT, request.getLatitude(), request.getLongitude());
        return new GoOutResponse(newLog.getId(), newLog.getEventTime());
    }

    private ComeBackResponse comeBack(UUID memberId, UUID memberPositionId, UUID companyId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);
        EventType lastEvent = getLastEventTypeToday(memberId);
        validateStateTransition(lastEvent, EventType.COME_BACK);

        AttendanceLog lastGoOut = attendanceLogRepository.findTopByMemberIdAndEventTypeOrderByEventTimeDesc(memberId, EventType.GO_OUT)
                .orElseThrow(() -> new BusinessException("외출 시작 기록을 찾을 수 없습니다."));

        // 오늘 날짜 외출 기록인지 확인
        if (!lastGoOut.getEventTime().toLocalDate().equals(today)) {
            throw new BusinessException("오늘 외출 기록이 없습니다. 외출 후 복귀해 주세요.");
        }

        LocalDateTime comeBackTime = LocalDateTime.now();
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), standardWorkPolicy, comeBackTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, comeBackTime, EventType.COME_BACK, request.getLatitude(), request.getLongitude());

        long goOutMinutes = java.time.Duration.between(lastGoOut.getEventTime(), comeBackTime).toMinutes();
        if (goOutMinutes < 0) {
            throw new BusinessException("복귀 시각이 외출 시각보다 이릅니다. 시스템 시간을 확인해주세요.");
        }

        dailyAttendance.addGoOutMinutes((int) goOutMinutes);
        Policy policy = policyAssignmentService.findEffectivePolicyForMemberByType(memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);
        attendanceValidator.validateGoOutTimePolicy(dailyAttendance, policy, (int) goOutMinutes);

        return new ComeBackResponse(newLog.getId(), newLog.getEventTime(), dailyAttendance.getTotalGoOutMinutes());
    }

    private BreakStartResponse breakStart(UUID memberId, UUID memberPositionId, UUID companyId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);
        EventType lastEvent = getLastEventTypeToday(memberId);
        validateStateTransition(lastEvent, EventType.BREAK_START);

        LocalDateTime breakStartTime = LocalDateTime.now();
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), standardWorkPolicy, breakStartTime, dailyAttendance.getFirstClockIn());
        attendanceValidator.validateBreakIsManualMode(standardWorkPolicy);

        AttendanceLog newLog = createAttendanceLog(memberId, breakStartTime, EventType.BREAK_START, request.getLatitude(), request.getLongitude());
        return new BreakStartResponse(newLog.getId(), newLog.getEventTime());
    }

    private BreakEndResponse breakEnd(UUID memberId, UUID memberPositionId, UUID companyId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);
        EventType lastEvent = getLastEventTypeToday(memberId);
        validateStateTransition(lastEvent, EventType.BREAK_END);

        AttendanceLog lastBreakStart = attendanceLogRepository.findTopByMemberIdAndEventTypeOrderByEventTimeDesc(memberId, EventType.BREAK_START)
                .orElseThrow(() -> new BusinessException("휴게 시작 기록을 찾을 수 없습니다."));

        // 오늘 날짜 휴게 기록인지 확인
        if (!lastBreakStart.getEventTime().toLocalDate().equals(today)) {
            throw new BusinessException("오늘 휴게 시작 기록이 없습니다. 휴게 시작 후 종료해 주세요.");
        }

        LocalDateTime breakEndTime = LocalDateTime.now();
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), standardWorkPolicy, breakEndTime, dailyAttendance.getFirstClockIn());
        attendanceValidator.validateBreakIsManualMode(standardWorkPolicy);

        AttendanceLog newLog = createAttendanceLog(memberId, breakEndTime, EventType.BREAK_END, request.getLatitude(), request.getLongitude());

        long breakMinutes = java.time.Duration.between(lastBreakStart.getEventTime(), breakEndTime).toMinutes();
        if (breakMinutes < 0) {
            throw new BusinessException("휴게 종료 시각이 휴게 시작 시각보다 이릅니다. 시스템 시간을 확인해주세요.");
        }

        dailyAttendance.addBreakMinutes((int) breakMinutes);
        Policy policy = policyAssignmentService.findEffectivePolicyForMemberByType(memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);
        attendanceValidator.validateBreakTimePolicy(dailyAttendance, policy, (int) breakMinutes);

        return new BreakEndResponse(newLog.getId(), newLog.getEventTime(), dailyAttendance.getTotalBreakMinutes());
    }

    private AttendanceLog createAttendanceLog(UUID memberId, LocalDateTime eventTime, EventType eventType, Double latitude, Double longitude) {
        AttendanceLog newLog = AttendanceLog.builder()
                .memberId(memberId)
                .eventType(eventType)
                .eventTime(eventTime)
                .latitude(latitude)
                .longitude(longitude)
                .isCorrected(false)
                .build();
        return attendanceLogRepository.save(newLog);
    }

    private DailyAttendance createDailyAttendance(UUID memberId, UUID companyId, LocalDate today, LocalDateTime clockInTime) {
        // TODO: Request 승인 시 DailyAttendance 생성 로직 구현 필요
        //  - 연차/병가/반차 Request 승인 시 해당 status로 DailyAttendance 미리 생성
        //  - 출근 시 기존 DailyAttendance 확인하여 update 또는 create
        //  - 현재는 출근 찍을 때만 NORMAL_WORK로 생성
        DailyAttendance dailyAttendance = DailyAttendance.builder()
                .memberId(memberId)
                .companyId(companyId)
                .attendanceDate(today)
                .status(AttendanceStatus.NORMAL_WORK)  // 임시: 출근 시 무조건 정상출근으로 설정
                .firstClockIn(clockInTime)
                .workedMinutes(0)
                .overtimeMinutes(0)
                .totalBreakMinutes(0)
                .build();
        return dailyAttendanceRepository.save(dailyAttendance);
    }

    // --- 이하 검증(validate) 관련 헬퍼 메서드들 ---
    private void validate(UUID memberId, UUID memberPositionId, UUID companyId, DeviceType deviceType, Double latitude, Double longitude, String clientIp, String wifiSsid) {
        // 기본근무 정책에서 AuthRule 조회
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);
        if (standardWorkPolicy != null) {
            PolicyRuleDetails ruleDetails = standardWorkPolicy.getRuleDetails();
            validateAuthRule(ruleDetails, deviceType, latitude, longitude, clientIp, wifiSsid);
        }
    }

    private Policy findActivePolicy(UUID companyId) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Policy> policyPage = policyRepository.findActivePolicies(companyId, LocalDate.now(), pageable);
        if (policyPage.isEmpty()) {
            throw new ResourceNotFoundException("적용된 출퇴근 정책이 없습니다.");
        }
        return policyPage.getContent().get(0);
    }

    /**
     * WorkLocation 기반 인증 규칙 검증 (디바이스 타입별)
     *
     * 디바이스 타입별 검증 방식:
     * - LAPTOP/DESKTOP: IP 주소 필수 (WiFi SSID 선택적 허용)
     * - MOBILE: GPS 또는 WiFi SSID 중 하나 이상
     *
     * 1. 허용된 근무지 목록 조회
     * 2. 디바이스 타입에 따라 필요한 인증 정보 확인
     * 3. 사용자의 현재 위치 정보로 매칭되는 근무지 찾기
     */
    private void validateAuthRule(PolicyRuleDetails ruleDetails, DeviceType deviceType, Double latitude, Double longitude, String clientIp, String wifiSsid) {
        // 로컬 개발 환경에서 근무지 인증 완전히 스킵
        if (skipLocationAuth) {
            log.debug("개발 모드: 근무지 인증 스킵 (skip-location-auth=true)");
            return;
        }

        if (ruleDetails == null || ruleDetails.getAuthRule() == null) {
            return; // 인증 규칙이 없으면 통과
        }

        AuthRuleDto authRule = ruleDetails.getAuthRule();

        // 허용된 근무지 ID가 없으면 통과
        if (authRule.getAllowedWorkLocationIds() == null || authRule.getAllowedWorkLocationIds().isEmpty()) {
            return;
        }

        // 허용된 근무지 목록 조회
        List<com.crewvy.workforce_service.attendance.entity.WorkLocation> allowedLocations =
                workLocationRepository.findAllById(authRule.getAllowedWorkLocationIds());

        // 정책에 설정된 근무지가 실제로 존재하지 않는 경우
        if (allowedLocations.isEmpty()) {
            log.error("정책에 설정된 근무지가 DB에 존재하지 않습니다. 설정된 ID: {}", authRule.getAllowedWorkLocationIds());
            throw new InvalidPolicyRuleException("정책에 설정된 근무지 정보를 찾을 수 없습니다. 관리자에게 문의하세요.");
        }

        // 정책에 설정된 ID 개수와 실제 조회된 개수가 다른 경우 (일부 근무지가 삭제됨)
        if (allowedLocations.size() < authRule.getAllowedWorkLocationIds().size()) {
            log.warn("정책에 설정된 근무지 중 일부가 DB에 존재하지 않습니다. 설정: {}, 조회: {}",
                    authRule.getAllowedWorkLocationIds().size(), allowedLocations.size());
        }

        // 디바이스 타입별 필수 인증 정보 검증
        if (deviceType == DeviceType.LAPTOP) {
            // 노트북/데스크톱: IP 주소 또는 WiFi 정보 필요
            if ((clientIp == null || clientIp.trim().isEmpty()) && (wifiSsid == null || wifiSsid.trim().isEmpty())) {
                throw new AuthenticationFailedException("노트북/데스크톱 출퇴근은 네트워크 정보(IP 주소 또는 WiFi)가 필요합니다.");
            }
        } else if (deviceType == DeviceType.MOBILE) {
            // 모바일: GPS 또는 WiFi 중 하나 필요
            boolean hasGps = (latitude != null && longitude != null);
            boolean hasWifi = (wifiSsid != null && !wifiSsid.trim().isEmpty());
            if (!hasGps && !hasWifi) {
                throw new AuthenticationFailedException("모바일 출퇴근은 GPS 위치 정보 또는 WiFi 네트워크 정보가 필요합니다.");
            }
        }

        // 사용자 위치/네트워크 정보로 매칭되는 근무지 찾기
        WorkLocation matchedLocation = null;
        List<String> failureReasons = new ArrayList<>();

        for (WorkLocation location : allowedLocations) {
            boolean isMatched = false;
            List<String> locationFailures = new ArrayList<>();

            if (deviceType == DeviceType.LAPTOP) {
                // 노트북/데스크톱: IP 또는 WiFi 매칭
                boolean ipMatched = isIpMatched(location, clientIp);
                boolean wifiMatched = isWifiMatched(location, wifiSsid);

                if (ipMatched || wifiMatched) {
                    isMatched = true;
                } else {
                    if (clientIp != null && !clientIp.trim().isEmpty()) {
                        locationFailures.add("IP 불일치");
                    }
                    if (wifiSsid != null && !wifiSsid.trim().isEmpty()) {
                        locationFailures.add("WiFi 불일치");
                    }
                }
            } else if (deviceType == DeviceType.MOBILE) {
                // 모바일: GPS 또는 WiFi 매칭
                boolean gpsMatched = isGpsMatched(location, latitude, longitude);
                boolean wifiMatched = isWifiMatched(location, wifiSsid);

                if (gpsMatched || wifiMatched) {
                    isMatched = true;
                } else {
                    if (latitude != null && longitude != null) {
                        locationFailures.add("GPS 범위 초과");
                    }
                    if (wifiSsid != null && !wifiSsid.trim().isEmpty()) {
                        locationFailures.add("WiFi 불일치");
                    }
                }
            }

            // 매칭되면 해당 근무지로 설정
            if (isMatched) {
                matchedLocation = location;
                break;
            } else {
                failureReasons.add(String.format("%s (%s)", location.getName(), String.join(", ", locationFailures)));
            }
        }

        // 매칭된 근무지가 없으면 인증 실패
        if (matchedLocation == null) {
            String deviceTypeStr = deviceType == DeviceType.MOBILE ? "모바일" : "노트북/데스크톱";
            String detailedMessage = String.format(
                "허용된 근무지에서의 인증에 실패했습니다.%n디바이스 타입: %s%n시도한 근무지: %s",
                deviceTypeStr,
                String.join(" / ", failureReasons)
            );
            log.warn("근무지 인증 실패 - 디바이스: {}, GPS({}, {}), IP({}), WiFi({}), 실패 상세: {}",
                    deviceType, latitude, longitude, clientIp, wifiSsid, String.join(" / ", failureReasons));
            throw new AuthenticationFailedException(detailedMessage);
        }

        log.info("근무지 인증 성공: {} ({}) - 디바이스: {}", matchedLocation.getName(), matchedLocation.getId(), deviceType);
    }

    /**
     * WorkLocation의 GPS 정보와 사용자 위치가 매칭되는지 확인
     */
    private boolean isGpsMatched(WorkLocation location, Double userLat, Double userLon) {
        // GPS 정보가 없으면 매칭 불가
        if (location.getLatitude() == null || location.getLongitude() == null || location.getGpsRadius() == null) {
            return false;
        }

        // 사용자 위치 정보가 없으면 매칭 불가
        if (userLat == null || userLon == null) {
            return false;
        }

        double distance = DistanceCalculator.calculateDistanceInMeters(
            location.getLatitude(), location.getLongitude(), userLat, userLon
        );

        return distance <= location.getGpsRadius();
    }

    /**
     * WorkLocation의 IP 정보와 사용자 IP가 매칭되는지 확인
     */
    private boolean isIpMatched(WorkLocation location, String clientIp) {
        if (location.getIpAddress() == null || location.getIpAddress().trim().isEmpty()) {
            return false;
        }

        if (clientIp == null || clientIp.trim().isEmpty()) {
            return false;
        }

        // 로컬 개발 환경 처리: 127.0.0.1, localhost, IPv6 loopback을 모두 동일하게 취급
        String normalizedClientIp = normalizeLocalhost(clientIp);
        String normalizedLocationIp = normalizeLocalhost(location.getIpAddress());

        if (devModeEnabled) {
            log.debug("IP 매칭 시도 - 근무지: {}, 등록IP: {} (정규화: {}), 클라이언트IP: {} (정규화: {})",
                    location.getName(), location.getIpAddress(), normalizedLocationIp, clientIp, normalizedClientIp);
        }

        // 단순 IP 주소 매칭 (향후 CIDR 대역 매칭 추가 가능)
        boolean matched = normalizedLocationIp.equals(normalizedClientIp);

        if (devModeEnabled && matched) {
            log.info("✓ IP 매칭 성공 - 근무지: {}, IP: {}", location.getName(), clientIp);
        }

        return matched;
    }

    /**
     * localhost 관련 IP 주소를 정규화
     * 127.0.0.1, 0:0:0:0:0:0:0:1, ::1 등을 "localhost"로 통일
     */
    private String normalizeLocalhost(String ip) {
        if (ip == null) {
            return ip;
        }

        String trimmed = ip.trim();

        // IPv4 localhost
        if ("127.0.0.1".equals(trimmed) || "localhost".equalsIgnoreCase(trimmed)) {
            return "localhost";
        }

        // IPv6 localhost 형식들
        if ("0:0:0:0:0:0:0:1".equals(trimmed) || "::1".equals(trimmed)) {
            return "localhost";
        }

        return trimmed;
    }

    /**
     * WorkLocation의 WiFi 정보와 사용자 WiFi가 매칭되는지 확인
     */
    private boolean isWifiMatched(WorkLocation location, String userWifiSsid) {
        if (location.getWifiSsid() == null || location.getWifiSsid().trim().isEmpty()) {
            return false;
        }

        if (userWifiSsid == null || userWifiSsid.trim().isEmpty()) {
            return false;
        }

        return location.getWifiSsid().equalsIgnoreCase(userWifiSsid);
    }

    /**
     * 기간별 전체 직원 일일 근태 조회 (급여 정산용)
     *
     * 현재 구현: COMPANY 레벨 전체 조회만 지원 (salary 권한 필수)
     * TODO: Phase 2 - INDIVIDUAL, TEAM, DEPARTMENT 레벨 추가
     *
     * @param memberPositionId 요청자 직책 ID
     * @param companyId 회사 ID
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 기간 내 모든 직원의 일일 근태 데이터
     */
    @Transactional(readOnly = true)
    public List<DailyAttendanceSummaryRes> getDailyAttendanceSummary(
            UUID memberPositionId, UUID companyId,
            LocalDate startDate, LocalDate endDate) {

        validateDateRange(startDate, endDate);

        // Phase 1: COMPANY 레벨 - 급여 담당자 전체 조회
        checkPermissionOrThrow(memberPositionId, "salary", "READ", "COMPANY", "급여 정산을 위한 근태 요약 데이터를 조회할 권한이 없습니다.");

        // TODO: Phase 2 - 개인/팀장 조회 기능 추가
        // if (targetMemberId != null) {
        //     // INDIVIDUAL: 본인 데이터만 조회
        //     checkPermissionOrThrow(memberPositionId, "READ", "INDIVIDUAL", "본인의 근태 데이터만 조회할 수 있습니다.");
        //     return getDailyAttendanceByMember(targetMemberId, startDate, endDate, companyId);
        // }
        //
        // // TEAM/DEPARTMENT: 소속 팀원 조회
        // // member-service에서 조직 계층 정보 필요
        // List<UUID> subordinateIds = memberClient.getSubordinates(memberPositionId);
        // if (!subordinateIds.isEmpty()) {
        //     return getDailyAttendanceByMembers(subordinateIds, startDate, endDate, companyId);
        // }

        // companyId 필터링 (Multi-tenant 보안)
        List<DailyAttendance> dailyAttendances = dailyAttendanceRepository.findAllByDateRangeAndCompany(companyId, startDate, endDate);

        // 직책 정보 조회 (member-service)
        List<UUID> memberIds = dailyAttendances.stream()
                .map(DailyAttendance::getMemberId)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, MemberPositionListRes> positionMap = new HashMap<>();
        if (!memberIds.isEmpty()) {
            IdListReq request = IdListReq.builder()
                    .uuidList(memberIds)
                    .build();
            try {
                ApiResponse<List<MemberPositionListRes>> response = memberClient.getDefaultPositionList(memberPositionId, request);
                if (response != null && response.getData() != null) {
                    positionMap = response.getData().stream()
                            .collect(Collectors.toMap(MemberPositionListRes::getMemberId, p -> p));
                }
            } catch (Exception e) {
                log.error("Failed to fetch position info from member-service", e);
            }
        }

        // 응답 조립 (근태 데이터 + 직책 정보)
        final Map<UUID, MemberPositionListRes> finalPositionMap = positionMap;
        return dailyAttendances.stream()
                .map(da -> {
                    MemberPositionListRes position = finalPositionMap.get(da.getMemberId());
                    return DailyAttendanceSummaryRes.builder()
                            .memberId(da.getMemberId())
                            .memberName(position != null ? position.getMemberName() : null)
                            .organizationName(position != null ? position.getOrganizationName() : null)
                            .titleName(position != null ? position.getTitleName() : null)
                            .attendanceDate(da.getAttendanceDate())
                            .status(da.getStatus().getCodeValue())
                            .statusName(da.getStatus().getCodeName())
                            .isPaid(da.getStatus().isPaid())
                            .firstClockIn(da.getFirstClockIn())
                            .lastClockOut(da.getLastClockOut())
                            .workedMinutes(da.getWorkedMinutes())
                            .overtimeMinutes(da.getOvertimeMinutes())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 연도별 전체 직원 잔여 일수 조회 (급여 정산용)
     *
     * 현재 구현: COMPANY 레벨 전체 조회만 지원 (salary 권한 필수)
     * TODO: Phase 2 - INDIVIDUAL 레벨 추가 (본인 잔여 일수 조회)
     *
     * @param memberPositionId 요청자 직책 ID
     * @param companyId 회사 ID
     * @param year 조회 연도
     * @return 해당 연도의 전체 직원 잔여 일수 데이터
     */
    @Transactional(readOnly = true)
    public List<MemberBalanceSummaryRes> getMemberBalanceSummary(
            UUID memberPositionId,
            UUID companyId,
            Integer year) {

        validateYear(year);

        // Phase 1: COMPANY 레벨 - 급여 담당자만 전체 조회 가능
        checkPermissionOrThrow(memberPositionId, "salary", "READ", "COMPANY", "급여 정산을 위한 잔여 일수 데이터를 조회할 권한이 없습니다.");

        // TODO: Phase 2 - 개인 조회 기능 추가
        // if (targetMemberId != null) {
        //     // INDIVIDUAL: 본인 잔여 일수만 조회
        //     checkPermissionOrThrow(memberPositionId, "READ", "INDIVIDUAL", "본인의 잔여 일수만 조회할 수 있습니다.");
        //     return getBalanceByMember(targetMemberId, year, companyId);
        // }

        // companyId 필터링 (Multi-tenant 보안)
        List<MemberBalance> balances = memberBalanceRepository.findAllByYearAndCompany(companyId, year);

        // 직책 정보 조회 (member-service)
        List<UUID> memberIds = balances.stream()
                .map(MemberBalance::getMemberId)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, MemberPositionListRes> positionMap = new HashMap<>();
        if (!memberIds.isEmpty()) {
            IdListReq request = IdListReq.builder()
                    .uuidList(memberIds)
                    .build();
            try {
                ApiResponse<List<MemberPositionListRes>> response = memberClient.getDefaultPositionList(memberPositionId, request);
                if (response != null && response.getData() != null) {
                    positionMap = response.getData().stream()
                            .collect(Collectors.toMap(MemberPositionListRes::getMemberId, p -> p));
                }
            } catch (Exception e) {
                log.error("Failed to fetch position info from member-service", e);
            }
        }

        // 응답 조립 (잔여 일수 데이터 + 직책 정보)
        final Map<UUID, MemberPositionListRes> finalPositionMap = positionMap;
        return balances.stream()
                .map(mb -> {
                    MemberPositionListRes position = finalPositionMap.get(mb.getMemberId());
                    return MemberBalanceSummaryRes.builder()
                            .memberId(mb.getMemberId())
                            .memberName(position != null ? position.getMemberName() : null)
                            .organizationName(position != null ? position.getOrganizationName() : null)
                            .titleName(position != null ? position.getTitleName() : null)
                            .policyTypeCode(mb.getBalanceTypeCode().getCodeValue())
                            .policyTypeName(mb.getBalanceTypeCode().getCodeName())
                            .totalGranted(mb.getTotalGranted())
                            .totalUsed(mb.getTotalUsed())
                            .remainingBalance(mb.getRemaining())
                            .isPaid(mb.getIsPaid())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 연차 현황 조회 (권한에 따라 조회 범위 자동 결정)
     * - COMPANY 권한: 전사 직원 연차 현황 조회
     * - TEAM/DEPARTMENT 권한: 요청자가 속한 조직 및 하위 조직 직원 연차 현황만 조회
     *
     * @param memberId 요청자 ID
     * @param memberPositionId 요청자 직책 ID
     * @param companyId 회사 ID
     * @param year 조회 연도
     * @return 권한 범위 내 직원들의 연차 현황 리스트
     */
    @Transactional(readOnly = true)
    public List<MemberBalanceSummaryRes> getLeaveBalanceStatus(
            UUID memberId,
            UUID memberPositionId,
            UUID companyId,
            Integer year) {

        validateYear(year);

        // 1. member-service에서 조직 트리 가져오기
        List<OrganizationNodeDto> organizationTree;
        try {
            ApiResponse<List<OrganizationNodeDto>> orgResponse = memberClient.getOrganization(memberId);
            if (orgResponse == null || orgResponse.getData() == null) {
                log.error("Failed to fetch organization tree from member-service");
                throw new BusinessException("조직 정보를 가져오는 데 실패했습니다.");
            }
            organizationTree = orgResponse.getData();
        } catch (Exception e) {
            log.error("Error fetching organization tree", e);
            throw new BusinessException("조직 정보를 가져오는 데 실패했습니다.");
        }

        // 2. 권한에 따라 조회 범위 결정
        final List<UUID> targetMemberIds;
        boolean hasCompanyPermission = hasPermission(memberPositionId, "attendance", "READ", "COMPANY");

        if (hasCompanyPermission) {
            // COMPANY 권한: 전사 모든 직원 조회
            log.info("User has COMPANY level permission - fetching all company members' leave balance");
            List<UUID> allMembers = new ArrayList<>();
            extractAllMemberIds(organizationTree, allMembers);
            targetMemberIds = allMembers;
        } else {
            // TEAM/DEPARTMENT 권한: 본인 조직 및 하위 조직 직원만 조회
            boolean hasTeamPermission = hasPermission(memberPositionId, "attendance", "READ", "TEAM");
            if (!hasTeamPermission) {
                throw new PermissionDeniedException("연차 현황을 조회할 권한이 없습니다.");
            }

            log.info("User has TEAM level permission - fetching organization members' leave balance only");
            UUID myOrganizationId = findMemberOrganization(organizationTree, memberId);
            if (myOrganizationId == null) {
                log.warn("Member {} not found in organization tree", memberId);
                return new ArrayList<>();
            }
            targetMemberIds = extractMemberIdsFromOrganization(organizationTree, myOrganizationId);
        }

        if (targetMemberIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 해당 연도의 MemberBalance 조회 (개선)
        List<MemberBalance> filteredBalances = memberBalanceRepository.findAllByMemberIdInAndYear(targetMemberIds, year);

        if (filteredBalances.isEmpty()) {
            return new ArrayList<>();
        }

        // 5. 직책 정보 조회 (member-service)
        List<UUID> memberIds = filteredBalances.stream()
                .map(MemberBalance::getMemberId)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, MemberPositionListRes> positionMap = new HashMap<>();
        if (!memberIds.isEmpty()) {
            IdListReq request = IdListReq.builder()
                    .uuidList(memberIds)
                    .build();
            try {
                ApiResponse<List<MemberPositionListRes>> response = memberClient.getDefaultPositionList(memberPositionId, request);
                if (response != null && response.getData() != null) {
                    positionMap = response.getData().stream()
                            .collect(Collectors.toMap(MemberPositionListRes::getMemberId, p -> p));
                }
            } catch (Exception e) {
                log.error("Failed to fetch position info from member-service", e);
            }
        }

        // 6. 응답 조립
        final Map<UUID, MemberPositionListRes> finalPositionMap = positionMap;
        return filteredBalances.stream()
                .map(mb -> {
                    MemberPositionListRes position = finalPositionMap.get(mb.getMemberId());
                    return MemberBalanceSummaryRes.builder()
                            .memberId(mb.getMemberId())
                            .memberName(position != null ? position.getMemberName() : null)
                            .organizationName(position != null ? position.getOrganizationName() : null)
                            .titleName(position != null ? position.getTitleName() : null)
                            .policyTypeCode(mb.getBalanceTypeCode().getCodeValue())
                            .policyTypeName(mb.getBalanceTypeCode().getCodeName())
                            .totalGranted(mb.getTotalGranted())
                            .totalUsed(mb.getTotalUsed())
                            .remainingBalance(mb.getRemaining())
                            .isPaid(mb.getIsPaid())
                            .isUsable(mb.getIsUsable())
                            .build();
                })
                .collect(Collectors.toList());
    }


    // --- 검증 메서드들 ---

    /**
     * 날짜 범위 유효성 검증
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일과 종료일은 필수입니다.");
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException("시작일은 종료일보다 이후일 수 없습니다.");
        }

        // 최대 조회 기간 제한 (예: 1년)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 365) {
            throw new InvalidDateRangeException("조회 기간은 최대 365일까지 가능합니다.");
        }
    }

    /**
     * 연도 유효성 검증
     */
    private void validateYear(Integer year) {
        if (year == null) {
            throw new IllegalArgumentException("연도는 필수입니다.");
        }

        int currentYear = LocalDate.now().getYear();

        // 너무 과거 연도 제한 (예: 10년 전까지)
        if (year < currentYear - 10) {
            throw new InvalidDateRangeException("조회 가능한 연도는 최근 10년 이내입니다.");
        }

        // 미래 연도 제한
        if (year > currentYear + 1) {
            throw new InvalidDateRangeException("미래 연도는 조회할 수 없습니다.");
        }
    }

    /**
     * 정책에서 기준 근무시간 추출
     */
    private Integer getStandardWorkMinutes(Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return 480; // 기본값: 8시간
        }

        PolicyRuleDetails ruleDetails = policy.getRuleDetails();
        if (ruleDetails.getWorkTimeRule() != null && ruleDetails.getWorkTimeRule().getFixedWorkMinutes() != null) {
            return ruleDetails.getWorkTimeRule().getFixedWorkMinutes();
        }

        return 480; // 기본값: 8시간
    }

    /**
     * 오늘 가장 최근 이벤트 타입 조회
     * @param memberId 직원 ID
     * @return 가장 최근 이벤트 타입 (없으면 null)
     */
    private EventType getLastEventTypeToday(UUID memberId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<AttendanceLog> logs = attendanceLogRepository.findByMemberIdAndEventTimeBetweenOrderByEventTimeDesc(
                memberId, startOfDay, endOfDay);

        if (logs.isEmpty()) {
            return null;
        }

        return logs.get(0).getEventType();
    }

    /**
     * 근무 시간 제한 검증 (모든 근태 기록에 적용)
     * WorkTimeRuleDto의 workEndTime과 ClockOutRuleDto 규칙에 따라 유연하게 처리
     *
     * @param attendanceDate 근태 날짜 (출근일 기준)
     * @param policy 적용할 정책
     * @param requestTime 요청 시각
     * @param clockInTime 출근 시각 (WORK_DURATION 타입 검증에 필요, 출근 전이면 null)
     */
    private void validateWorkingHoursLimit(LocalDate attendanceDate, Policy policy, LocalDateTime requestTime, LocalDateTime clockInTime) {
        if (policy == null || policy.getRuleDetails() == null || policy.getRuleDetails().getClockOutRule() == null) {
            return; // 정책이 없으면 검증 안 함
        }

        ClockOutRuleDto clockOutRule = policy.getRuleDetails().getClockOutRule();
        String limitType = clockOutRule.getLimitType();

        if (limitType == null) {
            return; // 제한 타입이 없으면 검증 안 함
        }

        switch (limitType) {
            case "FIXED_PLUS_HOURS":
                // 정규 퇴근 시간 + N시간 이후 불가
                if (clockOutRule.getMaxHoursAfterWorkEnd() != null && policy.getRuleDetails().getWorkTimeRule() != null) {
                    String workEndTime = policy.getRuleDetails().getWorkTimeRule().getWorkEndTime();
                    if (workEndTime != null) {
                        String[] timeParts = workEndTime.split(":");
                        int standardHour = Integer.parseInt(timeParts[0]);
                        int standardMinute = Integer.parseInt(timeParts[1]);
                        LocalDateTime standardEndTime = attendanceDate.atTime(standardHour, standardMinute);
                        LocalDateTime maxAllowedTime = standardEndTime.plusHours(clockOutRule.getMaxHoursAfterWorkEnd());

                        if (requestTime.isAfter(maxAllowedTime)) {
                            throw new BusinessException(String.format("근태 기록 가능 시간(%s)을 초과했습니다.", maxAllowedTime.toLocalTime()));
                        }
                    }
                }
                break;

            case "END_OF_DAY":
                // 당일 자정까지만 허용
                LocalDateTime endOfDay = attendanceDate.atTime(23, 59, 59);
                if (requestTime.isAfter(endOfDay)) {
                    throw new BusinessException("당일 자정 이후에는 근태 기록을 처리할 수 없습니다.");
                }
                break;

            case "WORK_DURATION":
                // 출근 시각 기준 + N시간
                if (clockOutRule.getMaxWorkDurationHours() != null && clockInTime != null) {
                    LocalDateTime maxAllowedTime = clockInTime.plusHours(clockOutRule.getMaxWorkDurationHours());
                    if (requestTime.isAfter(maxAllowedTime)) {
                        throw new BusinessException(String.format("최대 근무 시간(%d시간)을 초과했습니다.", clockOutRule.getMaxWorkDurationHours()));
                    }
                }
                break;

            default:
                // 알 수 없는 제한 타입은 무시
                break;
        }
    }

    /**
     * 퇴근 시간 제한 검증 (하위 호환성 유지)
     */
    private void validateClockOutTime(DailyAttendance dailyAttendance, Policy policy, LocalDateTime clockOutTime) {
        validateWorkingHoursLimit(
            dailyAttendance.getAttendanceDate(),
            policy,
            clockOutTime,
            dailyAttendance.getFirstClockIn()
        );
    }

    /**
     * 외출 시간 정책 검증
     */
    private void validateGoOutTimePolicy(UUID companyId, UUID memberId, UUID memberPositionId, int singleGoOutMinutes, int totalGoOutMinutes) {
        Policy policy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);
        if (policy == null || policy.getRuleDetails() == null || policy.getRuleDetails().getGoOutRule() == null) {
            return; // 정책이 없으면 검증 안 함
        }

        GoOutRuleDto goOutRule = policy.getRuleDetails().getGoOutRule();

        // 1회 최대 외출 시간 체크
        if (goOutRule.getMaxSingleGoOutMinutes() != null && singleGoOutMinutes > goOutRule.getMaxSingleGoOutMinutes()) {
            throw new BusinessException(String.format("1회 외출 시간이 최대 허용 시간(%d분)을 초과했습니다.", goOutRule.getMaxSingleGoOutMinutes()));
        }

        // 일일 최대 외출 시간 체크
        if (goOutRule.getMaxDailyGoOutMinutes() != null && totalGoOutMinutes > goOutRule.getMaxDailyGoOutMinutes()) {
            throw new BusinessException(String.format("일일 외출 시간이 최대 허용 시간(%d분)을 초과했습니다.", goOutRule.getMaxDailyGoOutMinutes()));
        }
    }

    /**
     * 휴게 시간 정책 검증 (휴게 종료 시 호출)
     */
    private void validateBreakTimePolicy(UUID companyId, UUID memberId, UUID memberPositionId, int totalBreakMinutes) {
        Policy policy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);
        if (policy == null || policy.getRuleDetails() == null || policy.getRuleDetails().getBreakRule() == null) {
            return; // 정책이 없으면 검증 안 함
        }

        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();

        // 일일 최대 휴게 시간 체크
        if (breakRule.getMaxDailyBreakMinutes() != null && totalBreakMinutes > breakRule.getMaxDailyBreakMinutes()) {
            throw new BusinessException(String.format("일일 휴게 시간이 최대 허용 시간(%d분)을 초과했습니다.", breakRule.getMaxDailyBreakMinutes()));
        }
    }

    /**
     * 법정 최소 휴게 시간 검증 (퇴근 시 호출)
     * 근로기준법 제54조: 4시간 근무 시 30분, 8시간 근무 시 1시간(60분) 이상 휴게 부여 의무

    /**
     * AUTO 모드일 경우 근무시간에 따라 자동으로 휴게 시간 계산
     * - 4시간 이상 ~ 8시간 미만: mandatoryBreakMinutes (기본 30분)
     * - 8시간 이상: defaultBreakMinutesFor8Hours (기본 60분)
     */
    private void autoCalculateBreakTime(DailyAttendance dailyAttendance, Policy policy, LocalDateTime clockOutTime) {
        if (policy == null || policy.getRuleDetails() == null || policy.getRuleDetails().getBreakRule() == null) {
            return;
        }

        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();

        // AUTO 모드가 아니면 계산하지 않음
        if (!"AUTO".equals(breakRule.getType())) {
            return;
        }

        // 이미 수동으로 휴게 시간이 기록된 경우 자동 계산하지 않음
        if (dailyAttendance.getTotalBreakMinutes() != null && dailyAttendance.getTotalBreakMinutes() > 0) {
            return;
        }

        LocalDateTime firstClockIn = dailyAttendance.getFirstClockIn();
        if (firstClockIn == null) {
            return;
        }

        // 총 경과 시간 계산 (출근 ~ 퇴근)
        Duration totalDuration = Duration.between(firstClockIn, clockOutTime);
        int totalMinutes = (int) totalDuration.toMinutes();

        // 근무 시간에 따라 자동으로 휴게 시간 설정
        int autoBreakMinutes = 0;
        if (totalMinutes >= 480) {
            // 8시간 이상 근무: defaultBreakMinutesFor8Hours (기본 60분)
            autoBreakMinutes = (breakRule.getDefaultBreakMinutesFor8Hours() != null)
                    ? breakRule.getDefaultBreakMinutesFor8Hours()
                    : 60;
        } else if (totalMinutes >= 240) {
            // 4시간 이상 근무: mandatoryBreakMinutes (기본 30분)
            autoBreakMinutes = (breakRule.getMandatoryBreakMinutes() != null)
                    ? breakRule.getMandatoryBreakMinutes()
                    : 30;
        }

        // 자동 계산된 휴게 시간 설정
        if (autoBreakMinutes > 0) {
            dailyAttendance.setTotalBreakMinutes(autoBreakMinutes);
            log.info("AUTO 모드: 자동으로 휴게 시간 {}분 차감 (총 근무: {}분)", autoBreakMinutes, totalMinutes);
        }
    }

    /**
     * 휴게 규칙 타입이 MANUAL인지 검증
     * AUTO 모드일 경우 수동 휴게 기록 불가
     */
    private void validateBreakIsManualMode(Policy policy) {
        if (policy == null || policy.getRuleDetails() == null || policy.getRuleDetails().getBreakRule() == null) {
            return; // 정책이 없으면 허용
        }

        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();
        
        if ("AUTO".equals(breakRule.getType())) {
            throw new BusinessException("자동 차감 모드가 설정되어 있어 수동으로 휴게 시간을 기록할 수 없습니다.");
        }
    }
    private void validateMandatoryBreakTime(DailyAttendance dailyAttendance, Policy policy) {
        if (policy == null || policy.getRuleDetails() == null || policy.getRuleDetails().getBreakRule() == null) {
            return; // 정책이 없으면 검증 안 함
        }

        BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();

        // AUTO 모드일 때만 검증 (MANUAL 모드는 사용자가 직접 관리)
        if (!"AUTO".equals(breakRule.getType())) {
            return;
        }

        // 근무 시간과 실제 휴게 시간
        Integer workedMinutes = dailyAttendance.getWorkedMinutes();
        Integer totalBreakMinutes = dailyAttendance.getTotalBreakMinutes();

        if (workedMinutes == null || totalBreakMinutes == null) {
            return; // 데이터가 없으면 검증 스킵
        }

        // 실제 총 근로 시간 = 근무 시간 + 휴게 시간
        int totalMinutes = workedMinutes + totalBreakMinutes;

        // 법정 최소 휴게 시간 계산
        int requiredBreakMinutes = 0;

        if (totalMinutes >= 480) {
            // 8시간 이상 근무: 60분 이상 휴게 필요
            requiredBreakMinutes = (breakRule.getDefaultBreakMinutesFor8Hours() != null)
                ? breakRule.getDefaultBreakMinutesFor8Hours()
                : 60; // 기본값: 법정 최소 60분
        } else if (totalMinutes >= 240) {
            // 4시간 이상 근무: 30분 이상 휴게 필요
            requiredBreakMinutes = (breakRule.getMandatoryBreakMinutes() != null)
                ? breakRule.getMandatoryBreakMinutes()
                : 30; // 기본값: 법정 최소 30분
        }

        // 법정 최소 휴게 시간 미달 체크
        if (requiredBreakMinutes > 0 && totalBreakMinutes < requiredBreakMinutes) {
            String workHours = String.format("%.1f", totalMinutes / 60.0);
            throw new BusinessException(
                String.format("근로기준법 위반: %s시간 근무 시 최소 %d분의 휴게 시간이 필요합니다. (현재: %d분)",
                    workHours, requiredBreakMinutes, totalBreakMinutes)
            );
        }
    }

    /**
     * 지각 여부 확인
     */
    private void checkLateness(DailyAttendance dailyAttendance, Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        PolicyRuleDetails ruleDetails = policy.getRuleDetails();
        if (ruleDetails.getWorkTimeRule() == null || ruleDetails.getWorkTimeRule().getWorkStartTime() == null) {
            return;
        }

        String workStartTime = ruleDetails.getWorkTimeRule().getWorkStartTime();
        Integer latenessGraceMinutes = null;

        if (ruleDetails.getLatenessRule() != null) {
            latenessGraceMinutes = ruleDetails.getLatenessRule().getLatenessGraceMinutes();
        }

        dailyAttendance.checkAndSetLateness(workStartTime, latenessGraceMinutes);
    }

    /**
     * 조퇴 여부 확인
     */
    private void checkEarlyLeave(DailyAttendance dailyAttendance, Policy policy) {
        if (policy == null || policy.getRuleDetails() == null) {
            return;
        }

        PolicyRuleDetails ruleDetails = policy.getRuleDetails();
        if (ruleDetails.getWorkTimeRule() == null || ruleDetails.getWorkTimeRule().getWorkEndTime() == null) {
            return;
        }

        String workEndTime = ruleDetails.getWorkTimeRule().getWorkEndTime();
        Integer earlyLeaveGraceMinutes = null;

        if (ruleDetails.getLatenessRule() != null) {
            earlyLeaveGraceMinutes = ruleDetails.getLatenessRule().getEarlyLeaveGraceMinutes();
        }

        dailyAttendance.checkAndSetEarlyLeave(workEndTime, earlyLeaveGraceMinutes);
    }

    /**
     * 오늘의 내 출퇴근 현황 조회
     */
    @Transactional(readOnly = true)
    public TodayAttendanceStatusResponse getMyTodayAttendance(UUID memberId) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .orElse(null);

        if (dailyAttendance == null) {
            return null;
        }

        EventType lastEventType = getLastEventTypeToday(memberId);
        return TodayAttendanceStatusResponse.from(dailyAttendance, lastEventType);
    }

    /**
     * 월별 내 출퇴근 현황 조회
     */
    @Transactional(readOnly = true)
    public List<DailyAttendance> getMyMonthlyAttendance(UUID memberId, UUID companyId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        return dailyAttendanceRepository.findAllByDateRangeAndCompany(companyId, startDate, endDate)
                .stream()
                .filter(da -> da.getMemberId().equals(memberId))
                .collect(Collectors.toList());
    }

    /**
     * 내 연차 잔여 일수 조회 (현재 연도 기준)
     */
    @Transactional(readOnly = true)
    public MemberBalance getMyBalance(UUID memberId) {
        int currentYear = LocalDate.now().getYear();
        return memberBalanceRepository.findByMemberIdAndBalanceTypeCodeAndYear(
                memberId,
                PolicyTypeCode.ANNUAL_LEAVE,
                currentYear
        ).orElse(null);
    }

    /**
     * 내 모든 휴가 정책 잔액 조회 (현재 연도 기준)
     * 연차, 병가, 육아휴직 등 부여받은 모든 정책의 잔액을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<MyBalanceRes> getMyAllBalances(UUID memberId, UUID memberPositionId, UUID companyId) {
        int currentYear = LocalDate.now().getYear();
        LocalDate yearDate = LocalDate.of(currentYear, 1, 1);
        LocalDateTime yearStart = yearDate.atStartOfDay();
        LocalDateTime yearEnd = yearDate.withDayOfYear(yearDate.lengthOfYear()).atTime(23, 59, 59);

        // 1. 실제 잔액 레코드가 있는 정책들 조회
        List<MemberBalance> balances = memberBalanceRepository.findAllByMemberIdAndYear(memberId, currentYear);

        // 2. 할당받은 모든 정책 조회
        List<Policy> allPolicies = policyAssignmentService.findAllAssignedPoliciesForMember(memberId, memberPositionId, companyId);

        // 3. 휴가/휴직 타입 정책만 필터링
        List<Policy> leavePolicies = allPolicies.stream()
                .filter(policy -> {
                    PolicyTypeCode typeCode = policy.getPolicyTypeCode();
                    return typeCode == PolicyTypeCode.ANNUAL_LEAVE
                            || typeCode == PolicyTypeCode.MATERNITY_LEAVE
                            || typeCode == PolicyTypeCode.PATERNITY_LEAVE
                            || typeCode == PolicyTypeCode.CHILDCARE_LEAVE
                            || typeCode == PolicyTypeCode.FAMILY_CARE_LEAVE
                            || typeCode == PolicyTypeCode.MENSTRUAL_LEAVE;
                })
                .toList();

        // 4. N+1 문제 해결: 정책별 사용 횟수와 차감일 합계를 한 번의 쿼리로 조회
        // PENDING과 APPROVED 모두 포함 (Request 생성 시 즉시 차감되므로)
        List<RequestStatus> activeStatuses = List.of(RequestStatus.PENDING, RequestStatus.APPROVED);

        Map<UUID, Long> approvedCountsMap = requestRepository
                .countApprovedRequestsForMemberAndYear(memberId, activeStatuses, yearStart, yearEnd)
                .stream()
                .collect(Collectors.toMap(com.crewvy.workforce_service.attendance.dto.query.PolicyUsageStats::getPolicyId, com.crewvy.workforce_service.attendance.dto.query.PolicyUsageStats::getCount));

        Map<UUID, Double> deductionDaysMap = requestRepository
                .sumDeductionDaysForMemberAndYear(memberId, activeStatuses, yearStart, yearEnd)
                .stream()
                .collect(Collectors.toMap(com.crewvy.workforce_service.attendance.dto.query.PolicyUsageStats::getPolicyId, com.crewvy.workforce_service.attendance.dto.query.PolicyUsageStats::getSum));

        // 5. 각 정책에 대해 잔액 정보 생성 (정책 기반)
        List<MyBalanceRes> resultList = new ArrayList<>();
        Set<PolicyTypeCode> processedTypeCodes = new HashSet<>();

        for (Policy policy : leavePolicies) {
            PolicyTypeCode typeCode = policy.getPolicyTypeCode();
            processedTypeCodes.add(typeCode);

            MemberBalance balance = balances.stream()
                    .filter(b -> b.getBalanceTypeCode().equals(typeCode))
                    .findFirst()
                    .orElse(null);

            boolean isBalanceDeductible = typeCode.name().contains("PTC00");

            Integer maxSplitCount = null;
            Integer currentSplitCount = null;
            if (policy.getRuleDetails() != null && policy.getRuleDetails().getLeaveRule() != null) {
                LeaveRuleDto leaveRule = policy.getRuleDetails().getLeaveRule();
                maxSplitCount = leaveRule.getMaxSplitCount();
                if (maxSplitCount != null) {
                    currentSplitCount = approvedCountsMap.getOrDefault(policy.getId(), 0L).intValue();
                }
            }

            if (balance != null) {
                // member_balance 레코드가 있는 경우
                resultList.add(MyBalanceRes.builder()
                        .balanceTypeCode(MyBalanceRes.BalanceTypeInfo.builder()
                                .codeValue(typeCode.getCodeValue())
                                .codeName(typeCode.getCodeName())
                                .isBalanceDeductible(isBalanceDeductible)
                                .build())
                        .year(balance.getYear())
                        .totalGranted(balance.getTotalGranted())
                        .totalUsed(balance.getTotalUsed())
                        .remaining(balance.getRemaining())
                        .expirationDate(balance.getExpirationDate())
                        .isPaid(policy.getIsPaid())
                        .maxSplitCount(maxSplitCount)
                        .currentSplitCount(currentSplitCount)
                        .build());
            } else {
                // member_balance가 없는 경우 정책 기반 fallback
                Double totalGranted = 0.0;
                if (policy.getRuleDetails() != null && policy.getRuleDetails().getLeaveRule() != null) {
                    Double defaultDays = policy.getRuleDetails().getLeaveRule().getDefaultDays();
                    if (defaultDays != null) {
                        totalGranted = defaultDays;
                    }
                }

                Double totalUsed = deductionDaysMap.getOrDefault(policy.getId(), 0.0);
                Double remaining = totalGranted - totalUsed;

                resultList.add(MyBalanceRes.builder()
                        .balanceTypeCode(MyBalanceRes.BalanceTypeInfo.builder()
                                .codeValue(typeCode.getCodeValue())
                                .codeName(typeCode.getCodeName())
                                .isBalanceDeductible(isBalanceDeductible)
                                .build())
                        .year(currentYear)
                        .totalGranted(totalGranted)
                        .totalUsed(totalUsed)
                        .remaining(remaining)
                        .expirationDate(null)
                        .isPaid(policy.getIsPaid())
                        .maxSplitCount(maxSplitCount)
                        .currentSplitCount(currentSplitCount)
                        .build());
            }
        }

        // 6. 정책 할당이 없지만 member_balance 레코드가 있는 경우 추가
        // (배치로 생성된 잔액은 있지만 정책 할당이 삭제된 경우)
        for (MemberBalance balance : balances) {
            if (!processedTypeCodes.contains(balance.getBalanceTypeCode())) {
                // 아직 처리하지 않은 타입 → 정책 없이 member_balance만 있는 경우
                PolicyTypeCode typeCode = balance.getBalanceTypeCode();
                boolean isBalanceDeductible = typeCode.name().contains("PTC00");

                resultList.add(MyBalanceRes.builder()
                        .balanceTypeCode(MyBalanceRes.BalanceTypeInfo.builder()
                                .codeValue(typeCode.getCodeValue())
                                .codeName(typeCode.getCodeName())
                                .isBalanceDeductible(isBalanceDeductible)
                                .build())
                        .year(balance.getYear())
                        .totalGranted(balance.getTotalGranted())
                        .totalUsed(balance.getTotalUsed())
                        .remaining(balance.getRemaining())
                        .expirationDate(balance.getExpirationDate())
                        .isPaid(balance.getIsPaid())
                        .maxSplitCount(null) // 정책이 없으므로
                        .currentSplitCount(null)
                        .build());
            }
        }

        return resultList;
    }



    /**
     * 내게 할당된 모든 정책 조회
     * 관리자가 할당한 정책만 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<AssignedPolicyRes> getMyAssignedPolicies(UUID memberId, UUID memberPositionId, UUID companyId) {
        List<Policy> policies = policyAssignmentService.findAllAssignedPoliciesForMember(memberId, memberPositionId, companyId);

        return policies.stream()
                .map(policy -> {
                    // allowedRequestUnits 추출 (leaveRule이 있는 경우만)
                    List<String> allowedRequestUnits = null;
                    if (policy.getRuleDetails() != null && policy.getRuleDetails().getLeaveRule() != null) {
                        allowedRequestUnits = policy.getRuleDetails().getLeaveRule().getAllowedRequestUnits();
                    }

                    return AssignedPolicyRes.builder()
                            .policyId(policy.getId())
                            .name(policy.getName())
                            .typeCode(policy.getPolicyTypeCode().getCodeValue())
                            .typeName(policy.getPolicyTypeCode().getCodeName())
                            .isActive(policy.getIsActive())
                            .allowedRequestUnits(allowedRequestUnits)
                            .ruleDetails(policy.getRuleDetails()) // 정책 규칙 상세 포함 (출장지 필터링 등에 사용)
                            .build();
                })
                .toList();
    }

    /**
     * 근태 현황 조회 (권한에 따라 조회 범위 자동 결정, 날짜 범위 및 페이징 지원)
     * - COMPANY 권한: 전사 직원 조회
     * - TEAM/DEPARTMENT 권한: 요청자가 속한 조직 및 하위 조직 직원 조회
     *
     * @param memberId 요청자 ID
     * @param memberPositionId 요청자 직책 ID
     * @param companyId 회사 ID
     * @param startDate 시작 날짜 (null이면 오늘)
     * @param endDate 종료 날짜 (null이면 오늘)
     * @param pageable 페이징 정보
     * @return 권한 범위 내 직원들의 근태 현황 페이지
     */
    @Transactional(readOnly = true)
    public Page<TeamMemberAttendanceRes> getTeamAttendanceStatus(UUID memberId, UUID memberPositionId, UUID companyId,
                                                                   LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("getTeamAttendanceStatus called with memberId={}, memberPositionId={}, companyId={}, startDate={}, endDate={}",
                memberId, memberPositionId, companyId, startDate, endDate);

        // 날짜 기본값 설정
        LocalDate queryStartDate = (startDate != null) ? startDate : LocalDate.now();
        LocalDate queryEndDate = (endDate != null) ? endDate : LocalDate.now();

        // 1. member-service에서 조직 트리 가져오기
        List<OrganizationNodeDto> organizationTree;
        try {
            log.info("Calling memberClient.getOrganization with memberId={}", memberId);
            ApiResponse<List<OrganizationNodeDto>> orgResponse = memberClient.getOrganization(memberId);
            log.info("Organization tree response: success={}, dataSize={}",
                    orgResponse != null && orgResponse.getData() != null,
                    orgResponse != null && orgResponse.getData() != null ? orgResponse.getData().size() : 0);
            if (orgResponse == null || orgResponse.getData() == null) {
                log.error("Failed to fetch organization tree from member-service: response is null or data is null");
                throw new BusinessException("조직 정보를 가져오는 데 실패했습니다.");
            }
            organizationTree = orgResponse.getData();
        } catch (Exception e) {
            log.error("Error fetching organization tree for memberId={}: {}", memberId, e.getMessage(), e);
            throw new BusinessException("조직 정보를 가져오는 데 실패했습니다.");
        }

        // 2. 권한에 따라 조회 범위 결정
        final List<UUID> targetMemberIds;
        boolean hasCompanyPermission = hasPermission(memberPositionId, "attendance", "READ", "COMPANY");

        if (hasCompanyPermission) {
            // COMPANY 권한: 전사 모든 직원 조회
            log.info("User has COMPANY level permission - fetching all company members");
            List<UUID> allMembers = new ArrayList<>();
            extractAllMemberIds(organizationTree, allMembers);
            targetMemberIds = allMembers;
        } else {
            // TEAM/DEPARTMENT 권한: 본인 조직 및 하위 조직 직원만 조회
            boolean hasTeamPermission = hasPermission(memberPositionId, "attendance", "READ", "TEAM");
            if (!hasTeamPermission) {
                throw new PermissionDeniedException("근태 현황을 조회할 권한이 없습니다.");
            }

            log.info("User has TEAM level permission - fetching organization members only");
            UUID myOrganizationId = findMemberOrganization(organizationTree, memberId);
            if (myOrganizationId == null) {
                log.warn("Member {} not found in organization tree", memberId);
                return Page.empty(pageable);
            }
            targetMemberIds = extractMemberIdsFromOrganization(organizationTree, myOrganizationId);
        }

        if (targetMemberIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 3. 날짜 범위 내의 DailyAttendance 조회
        List<DailyAttendance> attendances = dailyAttendanceRepository
                .findAllByMemberIdInAndAttendanceDateBetween(targetMemberIds, queryStartDate, queryEndDate);

        // 4. 승인된 Request 조회 (날짜 범위 내)
        LocalDateTime startDateTime = queryStartDate.atStartOfDay();
        LocalDateTime endDateTime = queryEndDate.atTime(23, 59, 59);
        List<Request> approvedRequests = requestRepository
                .findApprovedRequestsByMemberIdsAndDateRange(targetMemberIds, startDateTime, endDateTime);

        // memberId + date 조합을 키로 하는 Request Map 생성
        Map<String, Request> requestMap = approvedRequests.stream()
                .flatMap(req -> {
                    LocalDate start = req.getStartDateTime().toLocalDate();
                    LocalDate end = req.getEndDateTime().toLocalDate();
                    List<Map.Entry<String, Request>> entries = new ArrayList<>();
                    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                        String key = req.getMemberId() + "_" + date.toString();
                        entries.add(Map.entry(key, req));
                    }
                    return entries.stream();
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (r1, r2) -> r1));

        // 5. 직책 정보 조회 (member-service)
        Map<UUID, MemberPositionListRes> positionMap = new HashMap<>();
        if (!targetMemberIds.isEmpty()) {
            IdListReq request = IdListReq.builder()
                    .uuidList(targetMemberIds)
                    .build();
            try {
                ApiResponse<List<MemberPositionListRes>> response = memberClient.getDefaultPositionList(memberPositionId, request);
                if (response != null && response.getData() != null) {
                    positionMap = response.getData().stream()
                            .collect(Collectors.toMap(MemberPositionListRes::getMemberId, p -> p));
                }
            } catch (Exception e) {
                log.error("Failed to fetch position info from member-service", e);
            }
        }

        // 6. 활성화된 정책 할당 조회 (N+1 방지 위해 batch 조회)
        List<PolicyAssignment> activeAssignments = policyAssignmentRepository
                .findActiveAssignmentsByTargets(targetMemberIds, companyId, queryStartDate);

        // memberId별 정책 이름 Map 생성 (MEMBER 스코프 우선, 없으면 ORGANIZATION)
        Map<UUID, String> policyNameMap = new HashMap<>();
        for (PolicyAssignment assignment : activeAssignments) {
            UUID targetId = assignment.getTargetId();
            String policyName = assignment.getPolicy().getName();

            // MEMBER 스코프가 이미 있으면 건너뜀 (개인 정책 우선)
            if (assignment.getScopeType() == PolicyScopeType.MEMBER) {
                policyNameMap.put(targetId, policyName);
            } else if (!policyNameMap.containsKey(targetId)) {
                // ORGANIZATION 스코프는 개인 정책이 없을 때만 사용
                policyNameMap.put(targetId, policyName);
            }
        }

        // 7. 응답 데이터 조립 (모든 memberId x 날짜 조합 생성)
        final Map<UUID, MemberPositionListRes> finalPositionMap = positionMap;
        final Map<String, Request> finalRequestMap = requestMap;
        final Map<UUID, String> finalPolicyNameMap = policyNameMap;

        List<TeamMemberAttendanceRes> allResults = new ArrayList<>();

        for (UUID targetMemberId : targetMemberIds) {
            for (LocalDate date = queryStartDate; !date.isAfter(queryEndDate); date = date.plusDays(1)) {
                final LocalDate currentDate = date;
                MemberPositionListRes position = finalPositionMap.get(targetMemberId);

                // 해당 memberId + date의 DailyAttendance 찾기
                DailyAttendance attendance = attendances.stream()
                        .filter(da -> da.getMemberId().equals(targetMemberId) && da.getAttendanceDate().equals(currentDate))
                        .findFirst()
                        .orElse(null);

                // 해당 memberId + date의 Request 찾기
                String requestKey = targetMemberId + "_" + currentDate.toString();
                Request request = finalRequestMap.get(requestKey);

                String status = null;
                Boolean isLate = false;
                String clockInTime = "-";
                String clockOutTime = "-";
                String workHours = "-";
                String requestType = null;
                String requestReason = null;

                // Request 정보 우선 (승인된 휴가, 출장 등)
                if (request != null && request.getPolicy() != null) {
                    requestType = request.getPolicy().getPolicyTypeCode().getCodeName();
                    requestReason = request.getReason();
                    // Request가 있으면 status도 설정
                    status = requestType;
                }

                if (attendance != null) {
                    // 근태 상태 (한글명) - Request가 없을 때만 DailyAttendance의 상태 사용
                    if (status == null && attendance.getStatus() != null) {
                        status = attendance.getStatus().getCodeName(); // 한글 (예: "정상근무")
                    }

                        // 지각 여부
                        isLate = attendance.getIsLate() != null && attendance.getIsLate();

                        // 출근 시간 포맷팅
                        if (attendance.getFirstClockIn() != null) {
                            clockInTime = String.format("%02d:%02d",
                                    attendance.getFirstClockIn().getHour(),
                                    attendance.getFirstClockIn().getMinute());
                        }

                        // 퇴근 시간 포맷팅
                        if (attendance.getLastClockOut() != null) {
                            clockOutTime = String.format("%02d:%02d",
                                    attendance.getLastClockOut().getHour(),
                                    attendance.getLastClockOut().getMinute());
                        }

                        // 근무 시간 포맷팅
                        if (attendance.getWorkedMinutes() != null && attendance.getWorkedMinutes() > 0) {
                            int hours = attendance.getWorkedMinutes() / 60;
                            int minutes = attendance.getWorkedMinutes() % 60;
                            workHours = String.format("%d시간 %d분", hours, minutes);
                        }
                    }

                // status가 여전히 null이면 기본값 설정
                if (status == null) {
                    status = "미출근";
                }

                // 적용 정책 이름 가져오기
                String effectivePolicy = finalPolicyNameMap.getOrDefault(targetMemberId, "-");

                TeamMemberAttendanceRes res = TeamMemberAttendanceRes.builder()
                        .memberId(targetMemberId)
                        .name(position != null ? position.getMemberName() : "알 수 없음")
                        .department(position != null ? position.getOrganizationName() : "-")
                        .title(position != null ? position.getTitleName() : "-")
                        .date(currentDate.toString())
                        .status(status)
                        .isLate(isLate)
                        .clockInTime(clockInTime)
                        .clockOutTime(clockOutTime)
                        .workHours(workHours)
                        .effectivePolicy(effectivePolicy)
                        .requestType(requestType)
                        .requestReason(requestReason)
                        .build();

                allResults.add(res);
            }
        }

        // 7. 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allResults.size());

        List<TeamMemberAttendanceRes> pagedResults = allResults.subList(start, end);
        return new PageImpl<>(pagedResults, pageable, allResults.size());
    }

    // --- Private Helper Methods ---

    /**
     * 오늘 날짜의 출근 기록을 조회하거나, 없으면 예외를 발생시킵니다.
     */
    private DailyAttendance getTodaysAttendanceOrThrow(UUID memberId, LocalDate date) {
        return dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, date)
                .orElseThrow(() -> new ResourceNotFoundException("해당 날짜의 출근 기록이 없습니다."));
    }

    /**
     * 이전 이벤트 상태에 따라 새로운 이벤트 발생이 유효한지 검증합니다.
     */
    private void validateStateTransition(EventType lastEvent, EventType newEvent) {
        if (lastEvent == EventType.CLOCK_OUT) {
            // 퇴근 후에는 중복 퇴근(정책 허용 시) 외에 다른 이벤트 발생 불가
            if (newEvent != EventType.CLOCK_OUT) {
                throw new BusinessException("이미 퇴근한 상태에서는 다른 작업을 할 수 없습니다.");
            }
        }

        switch (newEvent) {
            case GO_OUT:
                if (lastEvent == EventType.GO_OUT) throw new BusinessException("이미 외출 중입니다.");
                if (lastEvent == EventType.BREAK_START) throw new BusinessException("휴게 중에는 외출할 수 없습니다.");
                break;
            case COME_BACK:
                if (lastEvent != EventType.GO_OUT) throw new BusinessException("외출 중인 상태가 아닙니다.");
                break;
            case BREAK_START:
                if (lastEvent == EventType.BREAK_START) throw new BusinessException("이미 휴게 중입니다.");
                if (lastEvent == EventType.GO_OUT) throw new BusinessException("외출 중에는 휴게를 시작할 수 없습니다.");
                break;
            case BREAK_END:
                if (lastEvent != EventType.BREAK_START) throw new BusinessException("휴게 중인 상태가 아닙니다.");
                break;
            case CLOCK_OUT:
                if (lastEvent == EventType.GO_OUT) throw new BusinessException("외출 중입니다. 복귀 후 퇴근해 주세요.");
                if (lastEvent == EventType.BREAK_START) throw new BusinessException("휴게 중입니다. 휴게 종료 후 퇴근해 주세요.");
                break;
            default:
                // CLOCK_IN 등 다른 이벤트는 각 메서드에서 개별 처리
                break;
        }
    }

    /**
     * 휴일 여부 확인 (주말 또는 CompanyHoliday)
     * @param companyId 회사 ID
     * @param date 확인할 날짜
     * @return 휴일이면 true, 평일이면 false
     */
    private boolean isHoliday(UUID companyId, LocalDate date) {
        // 1. 주말(토요일, 일요일) 확인
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true;
        }

        // 2. CompanyHoliday 확인
        return companyHolidayRepository.existsByCompanyIdAndHolidayDate(companyId, date);
    }

    public List<DailyAttendanceRes> getMemberAttendance(UUID companyId, LocalDate startDate, LocalDate endDate) {

        List<DailyAttendance> dailyAttendanceList
                = dailyAttendanceRepository.findAllByDateRangeAndCompany(companyId, startDate, endDate);

        Map<UUID, List<DailyAttendance>> groupedByMember = dailyAttendanceList.stream()
                .collect(Collectors.groupingBy(DailyAttendance::getMemberId));

        List<DailyAttendanceRes> dailyAttendanceResList = groupedByMember.entrySet().stream()
                .map(entry -> {
                    UUID memberId = entry.getKey();
                    List<DailyAttendance> memberAttendances = entry.getValue();

                    int sumWorkingDays = memberAttendances.size();

                    int sumOvertime = memberAttendances.stream()
                            .mapToInt(da -> da.getDaytimeOvertimeMinutes() != null ? da.getDaytimeOvertimeMinutes() : 0)
                            .sum();

                    int sumNight = memberAttendances.stream()
                            .mapToInt(da -> da.getNightWorkMinutes() != null ? da.getNightWorkMinutes() : 0)
                            .sum();

                    int sumHoliday = memberAttendances.stream()
                            .mapToInt(da -> da.getHolidayWorkMinutes() != null ? da.getHolidayWorkMinutes() : 0)
                            .sum();

                    return new DailyAttendanceRes(memberId, sumWorkingDays, sumOvertime, sumNight, sumHoliday);
                }).toList();

        return dailyAttendanceResList;

    }

    /**
     * 조직 트리에서 특정 멤버가 속한 조직 ID 찾기
     * @param organizationTree 전체 조직 트리
     * @param memberId 찾을 멤버 ID
     * @return 멤버가 속한 조직 ID (없으면 null)
     */
    private UUID findMemberOrganization(List<OrganizationNodeDto> organizationTree, UUID memberId) {
        for (OrganizationNodeDto node : organizationTree) {
            // 현재 노드의 멤버들 확인
            if (node.getMembers() != null) {
                boolean found = node.getMembers().stream()
                        .anyMatch(member -> member.getId().equals(memberId));
                if (found) {
                    return node.getId();
                }
            }
            // 자식 노드들 재귀 탐색
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                UUID result = findMemberOrganization(node.getChildren(), memberId);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 조직 트리에서 특정 조직 ID에 속한 모든 멤버 ID를 재귀적으로 추출
     * @param organizationTree 전체 조직 트리
     * @param targetOrganizationId 조회할 조직 ID
     * @return 해당 조직 및 하위 조직의 모든 멤버 ID 리스트
     */
    private List<UUID> extractMemberIdsFromOrganization(List<OrganizationNodeDto> organizationTree, UUID targetOrganizationId) {
        List<UUID> memberIds = new ArrayList<>();

        // 특정 조직 ID를 찾아서 해당 조직 및 하위 조직의 멤버 추출
        OrganizationNodeDto targetNode = findOrganizationNode(organizationTree, targetOrganizationId);
        if (targetNode != null) {
            extractAllMemberIds(List.of(targetNode), memberIds);
        }

        return memberIds;
    }

    /**
     * 조직 트리에서 특정 조직 ID를 가진 노드를 재귀적으로 찾기
     */
    private OrganizationNodeDto findOrganizationNode(List<OrganizationNodeDto> nodes, UUID targetId) {
        for (OrganizationNodeDto node : nodes) {
            if (node.getId().equals(targetId)) {
                return node;
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                OrganizationNodeDto found = findOrganizationNode(node.getChildren(), targetId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 조직 트리에서 모든 멤버 ID를 재귀적으로 추출
     */
    private void extractAllMemberIds(List<OrganizationNodeDto> nodes, List<UUID> memberIds) {
        for (OrganizationNodeDto node : nodes) {
            // 현재 노드의 멤버들 추가
            if (node.getMembers() != null) {
                node.getMembers().stream()
                        .map(MemberDto::getId)
                        .forEach(memberIds::add);
            }
            // 자식 노드들 재귀 탐색
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                extractAllMemberIds(node.getChildren(), memberIds);
            }
        }
    }

    /**
     * 근태 기록 수정 (관리자 전용)
     * TODO: 권한 검증 추가 필요
     */
    @Transactional
    public void updateDailyAttendance(UUID dailyAttendanceId, UUID memberPositionId,
                                      com.crewvy.workforce_service.attendance.dto.request.UpdateDailyAttendanceReq request) {
        // 1. 근태 기록 조회
        DailyAttendance dailyAttendance = dailyAttendanceRepository.findById(dailyAttendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("근태 기록을 찾을 수 없습니다."));

        // 2. 권한 검증 (TEAM 이상 권한 필요)
        boolean hasPermission = hasPermission(memberPositionId, "attendance", "UPDATE", "TEAM");
        if (!hasPermission) {
            throw new PermissionDeniedException("근태 기록을 수정할 권한이 없습니다.");
        }

        // 3. 데이터 검증
        validateUpdateRequest(request);

        // 4. 근태 기록 수정
        dailyAttendance.updateByAdmin(
                request.getFirstClockIn(),
                request.getLastClockOut(),
                request.getWorkedMinutes(),
                request.getOvertimeMinutes(),
                request.getTotalBreakMinutes(),
                request.getTotalGoOutMinutes(),
                request.getIsLate(),
                request.getLateMinutes(),
                request.getIsEarlyLeave(),
                request.getEarlyLeaveMinutes(),
                request.getStatus()
        );

        log.info("Daily attendance {} updated by admin. Comment: {}", dailyAttendanceId, request.getAdminComment());
    }

    /**
     * 근태 기록 수정 요청 검증
     */
    private void validateUpdateRequest(com.crewvy.workforce_service.attendance.dto.request.UpdateDailyAttendanceReq request) {
        // 출근 시각이 퇴근 시각보다 나중일 수 없음
        if (request.getFirstClockIn() != null && request.getLastClockOut() != null) {
            if (request.getFirstClockIn().isAfter(request.getLastClockOut())) {
                throw new BusinessException("출근 시각이 퇴근 시각보다 늦을 수 없습니다.");
            }
        }

        // 음수 값 검증 (이미 @Min 어노테이션으로 검증되지만 추가 검증)
        if (request.getWorkedMinutes() != null && request.getWorkedMinutes() < 0) {
            throw new BusinessException("근무 시간은 0분 이상이어야 합니다.");
        }
        if (request.getOvertimeMinutes() != null && request.getOvertimeMinutes() < 0) {
            throw new BusinessException("초과 근무 시간은 0분 이상이어야 합니다.");
        }
        if (request.getTotalBreakMinutes() != null && request.getTotalBreakMinutes() < 0) {
            throw new BusinessException("휴게 시간은 0분 이상이어야 합니다.");
        }
    }
}
