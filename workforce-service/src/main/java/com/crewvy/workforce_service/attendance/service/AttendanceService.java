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
import com.crewvy.workforce_service.feignClient.dto.response.MemberPositionListRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final MemberClient memberClient;
    private final PolicyAssignmentService policyAssignmentService;
    private final MemberBalanceRepository memberBalanceRepository;
    private final WorkLocationRepository workLocationRepository;
    private final CompanyHolidayRepository companyHolidayRepository;

    public ApiResponse<?> recordEvent(UUID memberId, UUID memberPositionId, UUID companyId, UUID organizationId, EventRequest request, String clientIp) {
//        checkPermissionOrThrow(memberPositionId, "attendance", "CREATE", "INDIVIDUAL", "근태를 기록할 권한이 없습니다.");

        // 인증/검증이 필요한 이벤트 그룹
        List<EventType> validationRequiredEvents = List.of(EventType.CLOCK_IN, EventType.CLOCK_OUT);

        if (validationRequiredEvents.contains(request.getEventType())) {
            validate(memberId, companyId, organizationId, request.getDeviceId(), request.getDeviceType(), request.getLatitude(), request.getLongitude(), clientIp, request.getWifiSsid());
        }

        switch (request.getEventType()) {
            case CLOCK_IN:
                ClockInResponse clockInResponse = clockIn(memberId, companyId, organizationId, request);
                return ApiResponse.success(clockInResponse, "출근 등록 완료.");
            case CLOCK_OUT:
                ClockOutResponse clockOutResponse = clockOut(memberId, companyId, organizationId, request);
                return ApiResponse.success(clockOutResponse, "퇴근 등록 완료.");
            case GO_OUT:
                GoOutResponse goOutResponse = goOut(memberId, organizationId, request);
                return ApiResponse.success(goOutResponse, "외출 등록 완료.");
            case COME_BACK:
                ComeBackResponse comeBackResponse = comeBack(memberId, organizationId, request);
                return ApiResponse.success(comeBackResponse, "복귀 등록 완료.");
            case BREAK_START:
                BreakStartResponse breakStartResponse = breakStart(memberId, organizationId, request);
                return ApiResponse.success(breakStartResponse, "휴게 시작 등록 완료.");
            case BREAK_END:
                BreakEndResponse breakEndResponse = breakEnd(memberId, organizationId, request);
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

    private ClockInResponse clockIn(UUID memberId, UUID companyId, UUID organizationId, EventRequest request) {
        LocalDate today = LocalDate.now();
        dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .ifPresent(d -> {
                    throw new DuplicateResourceException("이미 출근 처리되었습니다.");
                });

        LocalDateTime clockInTime = LocalDateTime.now();

        // 기본근무 정책 조회 (WorkTimeRule, LatenessRule 포함)
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, organizationId, companyId, PolicyTypeCode.STANDARD_WORK);

        // 근무 시간 제한 검증
        validateWorkingHoursLimit(today, standardWorkPolicy, clockInTime, null);

        AttendanceLog newLog = createAttendanceLog(memberId, clockInTime, EventType.CLOCK_IN, request.getLatitude(), request.getLongitude());

        DailyAttendance dailyAttendance;
        try {
            // DailyAttendance 생성 및 저장
            dailyAttendance = createDailyAttendance(memberId, companyId, today, clockInTime);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request created the record between the check and now.
            throw new DuplicateResourceException("이미 출근 처리되었습니다.");
        }

        // 지각 여부 판별
        checkLateness(dailyAttendance, standardWorkPolicy);

        return new ClockInResponse(newLog.getId(), newLog.getEventTime());
    }

    private ClockOutResponse clockOut(UUID memberId, UUID companyId, UUID organizationId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);

        EventType lastEvent = getLastEventTypeToday(memberId);
        validateStateTransition(lastEvent, EventType.CLOCK_OUT);

        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, organizationId, companyId, PolicyTypeCode.STANDARD_WORK);

        // 퇴근 중복 허용 정책 체크
        if (lastEvent == EventType.CLOCK_OUT) {
            ClockOutRuleDto clockOutRule = (standardWorkPolicy != null && standardWorkPolicy.getRuleDetails() != null)
                    ? standardWorkPolicy.getRuleDetails().getClockOutRule() : null;
            if (clockOutRule == null || !Boolean.TRUE.equals(clockOutRule.getAllowDuplicateClockOut())) {
                throw new BusinessException("이미 퇴근 처리되었습니다.");
            }
        }

        LocalDateTime clockOutTime = LocalDateTime.now();
        validateClockOutTime(dailyAttendance, standardWorkPolicy, clockOutTime);

        AttendanceLog newLog = createAttendanceLog(memberId, clockOutTime, EventType.CLOCK_OUT, request.getLatitude(), request.getLongitude());
        Integer standardWorkMinutes = getStandardWorkMinutes(standardWorkPolicy);

        // 휴일 여부 확인 (주말 또는 CompanyHoliday)
        boolean isHoliday = isHoliday(companyId, today);

        dailyAttendance.updateClockOut(clockOutTime, standardWorkMinutes, isHoliday);
        checkEarlyLeave(dailyAttendance, standardWorkPolicy);

        return new ClockOutResponse(
                newLog.getId(),
                newLog.getEventTime(),
                dailyAttendance.getWorkedMinutes(),
                dailyAttendance.getOvertimeMinutes()
        );
    }

    private GoOutResponse goOut(UUID memberId, UUID organizationId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);
        EventType lastEvent = getLastEventTypeToday(memberId);
        validateStateTransition(lastEvent, EventType.GO_OUT);

        LocalDateTime goOutTime = LocalDateTime.now();
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, organizationId, dailyAttendance.getCompanyId(), PolicyTypeCode.STANDARD_WORK);
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), standardWorkPolicy, goOutTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, goOutTime, EventType.GO_OUT, request.getLatitude(), request.getLongitude());
        return new GoOutResponse(newLog.getId(), newLog.getEventTime());
    }

    private ComeBackResponse comeBack(UUID memberId, UUID organizationId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);
        EventType lastEvent = getLastEventTypeToday(memberId);
        validateStateTransition(lastEvent, EventType.COME_BACK);

        AttendanceLog lastGoOut = attendanceLogRepository.findTopByMemberIdAndEventTypeOrderByEventTimeDesc(memberId, EventType.GO_OUT)
                .orElseThrow(() -> new BusinessException("외출 시작 기록을 찾을 수 없습니다."));

        LocalDateTime comeBackTime = LocalDateTime.now();
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, organizationId, dailyAttendance.getCompanyId(), PolicyTypeCode.STANDARD_WORK);
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), standardWorkPolicy, comeBackTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, comeBackTime, EventType.COME_BACK, request.getLatitude(), request.getLongitude());

        long goOutMinutes = java.time.Duration.between(lastGoOut.getEventTime(), comeBackTime).toMinutes();
        if (goOutMinutes < 0) {
            throw new BusinessException("복귀 시각이 외출 시각보다 이릅니다. 시스템 시간을 확인해주세요.");
        }

        dailyAttendance.addGoOutMinutes((int) goOutMinutes);
        validateGoOutTimePolicy(dailyAttendance.getCompanyId(), memberId, organizationId, (int) goOutMinutes, dailyAttendance.getTotalGoOutMinutes());

        return new ComeBackResponse(newLog.getId(), newLog.getEventTime(), dailyAttendance.getTotalGoOutMinutes());
    }

    private BreakStartResponse breakStart(UUID memberId, UUID organizationId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);
        EventType lastEvent = getLastEventTypeToday(memberId);
        validateStateTransition(lastEvent, EventType.BREAK_START);

        LocalDateTime breakStartTime = LocalDateTime.now();
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, organizationId, dailyAttendance.getCompanyId(), PolicyTypeCode.STANDARD_WORK);
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), standardWorkPolicy, breakStartTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, breakStartTime, EventType.BREAK_START, request.getLatitude(), request.getLongitude());
        return new BreakStartResponse(newLog.getId(), newLog.getEventTime());
    }

    private BreakEndResponse breakEnd(UUID memberId, UUID organizationId, EventRequest request) {
        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = getTodaysAttendanceOrThrow(memberId, today);
        EventType lastEvent = getLastEventTypeToday(memberId);
        validateStateTransition(lastEvent, EventType.BREAK_END);

        AttendanceLog lastBreakStart = attendanceLogRepository.findTopByMemberIdAndEventTypeOrderByEventTimeDesc(memberId, EventType.BREAK_START)
                .orElseThrow(() -> new BusinessException("휴게 시작 기록을 찾을 수 없습니다."));

        LocalDateTime breakEndTime = LocalDateTime.now();
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, organizationId, dailyAttendance.getCompanyId(), PolicyTypeCode.STANDARD_WORK);
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), standardWorkPolicy, breakEndTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, breakEndTime, EventType.BREAK_END, request.getLatitude(), request.getLongitude());

        long breakMinutes = java.time.Duration.between(lastBreakStart.getEventTime(), breakEndTime).toMinutes();
        if (breakMinutes < 0) {
            throw new BusinessException("휴게 종료 시각이 휴게 시작 시각보다 이릅니다. 시스템 시간을 확인해주세요.");
        }

        dailyAttendance.addBreakMinutes((int) breakMinutes);
        validateBreakTimePolicy(dailyAttendance.getCompanyId(), memberId, organizationId, dailyAttendance.getTotalBreakMinutes());

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
    private void validate(UUID memberId, UUID companyId, UUID organizationId, String deviceId, DeviceType deviceType, Double latitude, Double longitude, String clientIp, String wifiSsid) {
        validateApprovedDevice(deviceId, memberId, deviceType);
        // 기본근무 정책에서 AuthRule 조회
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, organizationId, companyId, PolicyTypeCode.STANDARD_WORK);
        if (standardWorkPolicy != null) {
            PolicyRuleDetails ruleDetails = standardWorkPolicy.getRuleDetails();
            validateAuthRule(ruleDetails, deviceType, latitude, longitude, clientIp, wifiSsid);
        }
    }

    private void validateApprovedDevice(String deviceId, UUID memberId, DeviceType deviceType) {
        if (deviceId == null) {
            throw new IllegalArgumentException("디바이스 ID가 없습니다.");
        }
        boolean isApproved = requestRepository.existsApprovedDevice(
                memberId,
                deviceId,
                deviceType,
                RequestStatus.APPROVED
        );
        if (!isApproved) {
            throw new UnauthorizedDeviceException("등록되지 않았거나 미승인된 디바이스입니다.");
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
     * WorkLocation 기반 인증 규칙 검증
     * 1. 허용된 근무지 목록 조회
     * 2. 사용자의 현재 위치 정보로 매칭되는 근무지 찾기
     * 3. 필수 인증 방식을 모두 만족하는지 확인
     */
    private void validateAuthRule(PolicyRuleDetails ruleDetails, DeviceType deviceType, Double latitude, Double longitude, String clientIp, String wifiSsid) {
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

        // 필수 인증 방식 목록
        List<String> requiredAuthTypes = authRule.getRequiredAuthTypes();
        if (requiredAuthTypes == null || requiredAuthTypes.isEmpty()) {
            requiredAuthTypes = List.of("GPS"); // 기본값: GPS
        }

        // 필수 인증 정보 누락 체크
        for (String authType : requiredAuthTypes) {
            switch (authType) {
                case "GPS":
                    if (latitude == null || longitude == null) {
                        throw new AuthenticationFailedException("GPS 위치 정보가 필요합니다. 위치 권한을 허용해주세요.");
                    }
                    break;
                case "IP":
                    if (clientIp == null || clientIp.trim().isEmpty()) {
                        log.error("클라이언트 IP 주소를 가져올 수 없습니다.");
                        throw new AuthenticationFailedException("IP 주소 정보를 가져올 수 없습니다.");
                    }
                    break;
                case "WIFI":
                    if (wifiSsid == null || wifiSsid.trim().isEmpty()) {
                        throw new AuthenticationFailedException("WiFi 네트워크 정보가 필요합니다. WiFi에 연결되어 있는지 확인해주세요.");
                    }
                    break;
                default:
                    log.error("정책에 알 수 없는 인증 방식이 설정되어 있습니다: {}", authType);
                    throw new InvalidPolicyRuleException("정책 설정 오류: 알 수 없는 인증 방식 (" + authType + ")");
            }
        }

        // 사용자 위치 정보로 매칭되는 근무지 찾기
        WorkLocation matchedLocation = null;
        List<String> failureReasons = new ArrayList<>();

        for (WorkLocation location : allowedLocations) {
            List<String> locationFailures = new ArrayList<>();
            boolean allAuthTypesMatch = true;

            // 각 필수 인증 방식에 대해 검증
            for (String authType : requiredAuthTypes) {
                switch (authType) {
                    case "GPS":
                        if (!isGpsMatched(location, latitude, longitude)) {
                            allAuthTypesMatch = false;
                            locationFailures.add("GPS 범위 초과");
                        }
                        break;
                    case "IP":
                        if (!isIpMatched(location, clientIp)) {
                            allAuthTypesMatch = false;
                            locationFailures.add("IP 불일치");
                        }
                        break;
                    case "WIFI":
                        if (!isWifiMatched(location, wifiSsid)) {
                            allAuthTypesMatch = false;
                            locationFailures.add("WiFi 불일치");
                        }
                        break;
                }
            }

            // 모든 인증 방식이 일치하면 매칭된 근무지로 설정
            if (allAuthTypesMatch) {
                matchedLocation = location;
                break;
            } else {
                failureReasons.add(String.format("%s (%s)", location.getName(), String.join(", ", locationFailures)));
            }
        }

        // 매칭된 근무지가 없으면 인증 실패
        if (matchedLocation == null) {
            String requiredMethodsStr = String.join(", ", requiredAuthTypes);
            String detailedMessage = String.format(
                "허용된 근무지에서의 인증에 실패했습니다.%n필수 인증: %s%n시도한 근무지: %s",
                requiredMethodsStr,
                String.join(" / ", failureReasons)
            );
            log.warn("근무지 인증 실패 - 사용자 정보: GPS({}, {}), IP({}), WiFi({}), 실패 상세: {}",
                    latitude, longitude, clientIp, wifiSsid, String.join(" / ", failureReasons));
            throw new AuthenticationFailedException(detailedMessage);
        }

        log.info("근무지 인증 성공: {} ({})", matchedLocation.getName(), matchedLocation.getId());
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

        // 단순 IP 주소 매칭 (향후 CIDR 대역 매칭 추가 가능)
        return location.getIpAddress().equals(clientIp);
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
    private void validateGoOutTimePolicy(UUID companyId, UUID memberId, UUID organizationId, int singleGoOutMinutes, int totalGoOutMinutes) {
        Policy policy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, organizationId, companyId, PolicyTypeCode.STANDARD_WORK);
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
     * 휴게 시간 정책 검증
     */
    private void validateBreakTimePolicy(UUID companyId, UUID memberId, UUID organizationId, int totalBreakMinutes) {
        Policy policy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, organizationId, companyId, PolicyTypeCode.STANDARD_WORK);
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
}
