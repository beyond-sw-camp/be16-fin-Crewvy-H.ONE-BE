package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.*;
import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.EventType;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.dto.request.EventRequest;
import com.crewvy.workforce_service.attendance.dto.response.*;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.AttendanceLog;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.repository.*;
import com.crewvy.workforce_service.attendance.util.DistanceCalculator;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.PositionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    public ApiResponse<?> recordEvent(UUID memberId, UUID memberPositionId, UUID companyId, EventRequest request, String clientIp) {
        // TODO: 테스트 후 주석 해제 필요
        // checkPermissionOrThrow(memberPositionId, "attendance", "CREATE", "INDIVIDUAL", "근태를 기록할 권한이 없습니다.");

        // 인증/검증이 필요한 이벤트 그룹
        List<EventType> validationRequiredEvents = List.of(EventType.CLOCK_IN, EventType.CLOCK_OUT);

        if (validationRequiredEvents.contains(request.getEventType())) {
            validate(memberId, companyId, request.getDeviceId(), request.getDeviceType(), request.getLatitude(), request.getLongitude(), clientIp);
        }

        switch (request.getEventType()) {
            case CLOCK_IN:
                ClockInResponse clockInResponse = clockIn(memberId, companyId, request);
                return ApiResponse.success(clockInResponse, "출근 등록 완료.");
            case CLOCK_OUT:
                ClockOutResponse clockOutResponse = clockOut(memberId, companyId, request);
                return ApiResponse.success(clockOutResponse, "퇴근 등록 완료.");
            case GO_OUT:
                GoOutResponse goOutResponse = goOut(memberId, request);
                return ApiResponse.success(goOutResponse, "외출 등록 완료.");
            case COME_BACK:
                ComeBackResponse comeBackResponse = comeBack(memberId, request);
                return ApiResponse.success(comeBackResponse, "복귀 등록 완료.");
            case BREAK_START:
                BreakStartResponse breakStartResponse = breakStart(memberId, request);
                return ApiResponse.success(breakStartResponse, "휴게 시작 등록 완료.");
            case BREAK_END:
                BreakEndResponse breakEndResponse = breakEnd(memberId, request);
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

    private ClockInResponse clockIn(UUID memberId, UUID companyId, EventRequest request) {
        LocalDate today = LocalDate.now();
        dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .ifPresent(d -> {
                    throw new DuplicateResourceException("이미 출근 처리되었습니다.");
                });

        LocalDateTime clockInTime = LocalDateTime.now();

        // 정책 조회
        Policy activePolicy = policyAssignmentService.findEffectivePolicyForMember(memberId, companyId);

        // 근무 시간 제한 검증
        validateWorkingHoursLimit(today, activePolicy, clockInTime, null);

        AttendanceLog newLog = createAttendanceLog(memberId, clockInTime, EventType.CLOCK_IN, request.getLatitude(), request.getLongitude());

        // DailyAttendance 생성
        DailyAttendance dailyAttendance = createDailyAttendance(memberId, companyId, today, clockInTime);

        // 지각 여부 판별
        checkLateness(dailyAttendance, activePolicy);

        return new ClockInResponse(newLog.getId(), newLog.getEventTime());
    }

    private ClockOutResponse clockOut(UUID memberId, UUID companyId, EventRequest request) {
        LocalDate today = LocalDate.now();

        // 출근 기록 확인
        DailyAttendance dailyAttendance = dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .orElseThrow(() -> new ResourceNotFoundException("출근 기록이 없습니다. 퇴근 처리할 수 없습니다."));

        // 정책 조회
        Policy activePolicy = policyAssignmentService.findEffectivePolicyForMember(memberId, companyId);

        // attendance_log에서 가장 최근 이벤트 조회하여 상태 확인
        EventType lastEvent = getLastEventTypeToday(memberId);

        // 퇴근 중복 체크 (정책에 따라)
        if (lastEvent == EventType.CLOCK_OUT) {
            ClockOutRuleDto clockOutRule = activePolicy != null && activePolicy.getRuleDetails() != null
                    ? activePolicy.getRuleDetails().getClockOutRule()
                    : null;

            // 퇴근 중복 허용 여부 확인
            if (clockOutRule == null || !Boolean.TRUE.equals(clockOutRule.getAllowDuplicateClockOut())) {
                throw new BusinessException("이미 퇴근 처리되었습니다.");
            }
            // 중복 허용이면 계속 진행 (마지막 퇴근 시각으로 업데이트)
        }

        // 외출 중에는 퇴근 불가
        if (lastEvent == EventType.GO_OUT) {
            throw new BusinessException("외출 중입니다. 복귀 처리 후 퇴근할 수 있습니다.");
        }

        // 휴게 중에는 퇴근 불가
        if (lastEvent == EventType.BREAK_START) {
            throw new BusinessException("휴게 중입니다. 휴게 종료 후 퇴근할 수 있습니다.");
        }

        LocalDateTime clockOutTime = LocalDateTime.now();

        // 퇴근 시간 제한 검증
        validateClockOutTime(dailyAttendance, activePolicy, clockOutTime);

        AttendanceLog newLog = createAttendanceLog(memberId, clockOutTime, EventType.CLOCK_OUT, request.getLatitude(), request.getLongitude());

        // 기준 근무시간 가져오기
        Integer standardWorkMinutes = getStandardWorkMinutes(activePolicy);

        dailyAttendance.updateClockOut(clockOutTime, standardWorkMinutes);

        // 조퇴 여부 판별
        checkEarlyLeave(dailyAttendance, activePolicy);

        return new ClockOutResponse(
                newLog.getId(),
                newLog.getEventTime(),
                dailyAttendance.getWorkedMinutes(),
                dailyAttendance.getOvertimeMinutes()
        );
    }

    private GoOutResponse goOut(UUID memberId, EventRequest request) {
        LocalDate today = LocalDate.now();

        // 출근 기록 확인
        DailyAttendance dailyAttendance = dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .orElseThrow(() -> new ResourceNotFoundException("출근 기록이 없습니다. 외출 처리할 수 없습니다."));

        // attendance_log에서 가장 최근 이벤트 조회하여 상태 확인
        EventType lastEvent = getLastEventTypeToday(memberId);

        // 퇴근 후에는 외출 불가
        if (lastEvent == EventType.CLOCK_OUT) {
            throw new BusinessException("이미 퇴근 처리되었습니다. 외출할 수 없습니다.");
        }

        // 외출 중복 체크
        if (lastEvent == EventType.GO_OUT) {
            throw new BusinessException("이미 외출 중입니다. 복귀 처리 후 다시 외출할 수 있습니다.");
        }

        // 휴게 중에는 외출 불가
        if (lastEvent == EventType.BREAK_START) {
            throw new BusinessException("휴게 중에는 외출할 수 없습니다. 휴게를 종료해주세요.");
        }

        LocalDateTime goOutTime = LocalDateTime.now();

        // 정책 조회 및 근무 시간 제한 검증
        Policy activePolicy = policyAssignmentService.findEffectivePolicyForMember(memberId, dailyAttendance.getCompanyId());
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), activePolicy, goOutTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, goOutTime, EventType.GO_OUT, request.getLatitude(), request.getLongitude());

        return new GoOutResponse(newLog.getId(), newLog.getEventTime());
    }

    private ComeBackResponse comeBack(UUID memberId, EventRequest request) {
        LocalDate today = LocalDate.now();

        // 출근 기록 확인
        DailyAttendance dailyAttendance = dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .orElseThrow(() -> new ResourceNotFoundException("출근 기록이 없습니다. 복귀 처리할 수 없습니다."));

        // attendance_log에서 가장 최근 이벤트 조회하여 상태 확인
        EventType lastEvent = getLastEventTypeToday(memberId);

        // 퇴근 후에는 복귀 불가
        if (lastEvent == EventType.CLOCK_OUT) {
            throw new BusinessException("이미 퇴근 처리되었습니다. 복귀할 수 없습니다.");
        }

        // 외출 상태 확인
        if (lastEvent != EventType.GO_OUT) {
            throw new BusinessException("외출 중이 아닙니다. 외출 시작 후 복귀할 수 있습니다.");
        }

        // attendance_log에서 가장 최근 GO_OUT 이벤트 조회 (시간 계산용)
        AttendanceLog lastGoOut = attendanceLogRepository.findTopByMemberIdAndEventTypeOrderByEventTimeDesc(memberId, EventType.GO_OUT)
                .orElseThrow(() -> new BusinessException("외출 기록을 찾을 수 없습니다."));

        LocalDateTime comeBackTime = LocalDateTime.now();

        // 정책 조회 및 근무 시간 제한 검증
        Policy activePolicy = policyAssignmentService.findEffectivePolicyForMember(memberId, dailyAttendance.getCompanyId());
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), activePolicy, comeBackTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, comeBackTime, EventType.COME_BACK, request.getLatitude(), request.getLongitude());

        // 외출 시간 계산
        long goOutMinutes = java.time.Duration.between(lastGoOut.getEventTime(), comeBackTime).toMinutes();

        // 음수 시간 체크
        if (goOutMinutes < 0) {
            throw new BusinessException("복귀 시각이 외출 시각보다 이릅니다. 시스템 시간을 확인해주세요.");
        }

        // 외출 시간 누적
        dailyAttendance.addGoOutMinutes((int) goOutMinutes);

        // 외출 시간 정책 검증 (정책이 있으면)
        validateGoOutTimePolicy(dailyAttendance.getCompanyId(), memberId, (int) goOutMinutes, dailyAttendance.getTotalGoOutMinutes());

        return new ComeBackResponse(newLog.getId(), newLog.getEventTime(), dailyAttendance.getTotalGoOutMinutes());
    }

    private BreakStartResponse breakStart(UUID memberId, EventRequest request) {
        LocalDate today = LocalDate.now();

        // 출근 기록 확인
        DailyAttendance dailyAttendance = dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .orElseThrow(() -> new ResourceNotFoundException("출근 기록이 없습니다. 휴게 시작 처리할 수 없습니다."));

        // attendance_log에서 가장 최근 이벤트 조회하여 상태 확인
        EventType lastEvent = getLastEventTypeToday(memberId);

        // 퇴근 후에는 휴게 불가
        if (lastEvent == EventType.CLOCK_OUT) {
            throw new BusinessException("이미 퇴근 처리되었습니다. 휴게를 시작할 수 없습니다.");
        }

        // 휴게 중복 체크
        if (lastEvent == EventType.BREAK_START) {
            throw new BusinessException("이미 휴게 중입니다. 휴게 종료 후 다시 시작할 수 있습니다.");
        }

        // 외출 중에는 휴게 불가
        if (lastEvent == EventType.GO_OUT) {
            throw new BusinessException("외출 중에는 휴게를 시작할 수 없습니다. 복귀 후 휴게를 시작해주세요.");
        }

        LocalDateTime breakStartTime = LocalDateTime.now();

        // 정책 조회 및 근무 시간 제한 검증
        Policy activePolicy = policyAssignmentService.findEffectivePolicyForMember(memberId, dailyAttendance.getCompanyId());
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), activePolicy, breakStartTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, breakStartTime, EventType.BREAK_START, request.getLatitude(), request.getLongitude());

        return new BreakStartResponse(newLog.getId(), newLog.getEventTime());
    }

    private BreakEndResponse breakEnd(UUID memberId, EventRequest request) {
        LocalDate today = LocalDate.now();

        // 출근 기록 확인
        DailyAttendance dailyAttendance = dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .orElseThrow(() -> new ResourceNotFoundException("출근 기록이 없습니다. 휴게 종료 처리할 수 없습니다."));

        // attendance_log에서 가장 최근 이벤트 조회하여 상태 확인
        EventType lastEvent = getLastEventTypeToday(memberId);

        // 퇴근 후에는 휴게 종료 불가
        if (lastEvent == EventType.CLOCK_OUT) {
            throw new BusinessException("이미 퇴근 처리되었습니다. 휴게를 종료할 수 없습니다.");
        }

        // 휴게 상태 확인
        if (lastEvent != EventType.BREAK_START) {
            throw new BusinessException("휴게 중이 아닙니다. 휴게 시작 후 종료할 수 있습니다.");
        }

        // attendance_log에서 가장 최근 BREAK_START 이벤트 조회 (시간 계산용)
        AttendanceLog lastBreakStart = attendanceLogRepository.findTopByMemberIdAndEventTypeOrderByEventTimeDesc(memberId, EventType.BREAK_START)
                .orElseThrow(() -> new BusinessException("휴게 시작 기록을 찾을 수 없습니다."));

        LocalDateTime breakEndTime = LocalDateTime.now();

        // 정책 조회 및 근무 시간 제한 검증
        Policy activePolicy = policyAssignmentService.findEffectivePolicyForMember(memberId, dailyAttendance.getCompanyId());
        validateWorkingHoursLimit(dailyAttendance.getAttendanceDate(), activePolicy, breakEndTime, dailyAttendance.getFirstClockIn());

        AttendanceLog newLog = createAttendanceLog(memberId, breakEndTime, EventType.BREAK_END, request.getLatitude(), request.getLongitude());

        // 휴게 시간 계산
        long breakMinutes = java.time.Duration.between(lastBreakStart.getEventTime(), breakEndTime).toMinutes();

        // 음수 시간 체크
        if (breakMinutes < 0) {
            throw new BusinessException("휴게 종료 시각이 휴게 시작 시각보다 이릅니다. 시스템 시간을 확인해주세요.");
        }

        // 휴게 시간 누적
        dailyAttendance.addBreakMinutes((int) breakMinutes);

        // 휴게 시간 정책 검증 (정책이 있으면)
        validateBreakTimePolicy(dailyAttendance.getCompanyId(), memberId, dailyAttendance.getTotalBreakMinutes());

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
    private void validate(UUID memberId, UUID companyId, String deviceId, DeviceType deviceType, Double latitude, Double longitude, String clientIp) {
        validateApprovedDevice(deviceId, memberId, deviceType);
        Policy activePolicy = policyAssignmentService.findEffectivePolicyForMember(memberId, companyId);
        PolicyRuleDetails ruleDetails = activePolicy.getRuleDetails();
        validateAuthRule(ruleDetails, deviceType, latitude, longitude, clientIp);
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

    private void validateAuthRule(PolicyRuleDetails ruleDetails, DeviceType deviceType, Double latitude, Double longitude, String clientIp) {
        if (ruleDetails == null || ruleDetails.getAuthRule() == null || ruleDetails.getAuthRule().getMethods() == null) {
            return; // 인증 규칙이 없으면 통과
        }

        AuthMethodDto applicableMethod = ruleDetails.getAuthRule().getMethods().stream()
                .filter(method -> deviceType.equals(method.getDeviceType()))
                .findFirst()
                .orElseThrow(() -> new InvalidPolicyRuleException("현재 기기에서 지원하는 인증 방식이 정책에 없습니다."));

        String authMethod = applicableMethod.getAuthMethod();
        Map<String, Object> details = applicableMethod.getDetails();

        switch (authMethod) {
            case "GPS":
                if (latitude == null || longitude == null) {
                    throw new IllegalArgumentException("GPS 인증 방식에는 좌표 정보가 필수입니다.");
                }
                validateGpsLocation(details, latitude, longitude);
                break;
            case "NETWORK_IP":
                validateIpAddress(details, clientIp);
                break;
            default:
                throw new InvalidPolicyRuleException("정책에 알 수 없는 인증 방식이 설정되어 있습니다.");
        }
    }

    private void validateGpsLocation(Map<String, Object> rules, double userLat, double userLon) {
        double gpsRadius = ((Number) rules.get("gpsRadiusMeters")).doubleValue();
        double officeLat = ((Number) rules.get("officeLatitude")).doubleValue();
        double officeLon = ((Number) rules.get("officeLongitude")).doubleValue();
        double distance = DistanceCalculator.calculateDistanceInMeters(officeLat, officeLon, userLat, userLon);
        if (distance > gpsRadius) {
            throw new AuthenticationFailedException(String.format("지정된 근무지로부터 약 %.0f미터 벗어났습니다.", distance));
        }
    }

    private void validateIpAddress(Map<String, Object> rules, String clientIp) {
        List<String> allowedIps = (List<String>) rules.get("allowedIps");
        if (allowedIps == null || allowedIps.isEmpty()) {
            throw new InvalidPolicyRuleException("정책에 허용된 IP가 등록되지 않았습니다.");
        }
        if (!allowedIps.contains(clientIp)) {
            throw new AuthenticationFailedException("허용되지 않은 IP입니다.");
        }
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

        Map<UUID, PositionDto> positionMap = new HashMap<>();
        if (!memberIds.isEmpty()) {
            IdListReq request = IdListReq.builder()
                    .uuidList(memberIds)
                    .build();
            try {
                ApiResponse<List<PositionDto>> response = memberClient.getDefaultPositionList(memberPositionId, request);
                if (response != null && response.getData() != null) {
                    positionMap = response.getData().stream()
                            .collect(Collectors.toMap(PositionDto::getMemberId, p -> p));
                }
            } catch (Exception e) {
                log.error("Failed to fetch position info from member-service", e);
            }
        }

        // 응답 조립 (근태 데이터 + 직책 정보)
        final Map<UUID, PositionDto> finalPositionMap = positionMap;
        return dailyAttendances.stream()
                .map(da -> {
                    PositionDto position = finalPositionMap.get(da.getMemberId());
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

        Map<UUID, PositionDto> positionMap = new HashMap<>();
        if (!memberIds.isEmpty()) {
            IdListReq request = IdListReq.builder()
                    .uuidList(memberIds)
                    .build();
            try {
                ApiResponse<List<PositionDto>> response = memberClient.getDefaultPositionList(memberPositionId, request);
                if (response != null && response.getData() != null) {
                    positionMap = response.getData().stream()
                            .collect(Collectors.toMap(PositionDto::getMemberId, p -> p));
                }
            } catch (Exception e) {
                log.error("Failed to fetch position info from member-service", e);
            }
        }

        // 응답 조립 (잔여 일수 데이터 + 직책 정보)
        final Map<UUID, PositionDto> finalPositionMap = positionMap;
        return balances.stream()
                .map(mb -> {
                    PositionDto position = finalPositionMap.get(mb.getMemberId());
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
    private void validateGoOutTimePolicy(UUID companyId, UUID memberId, int singleGoOutMinutes, int totalGoOutMinutes) {
        Policy policy = policyAssignmentService.findEffectivePolicyForMember(memberId, companyId);
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
    private void validateBreakTimePolicy(UUID companyId, UUID memberId, int totalBreakMinutes) {
        Policy policy = policyAssignmentService.findEffectivePolicyForMember(memberId, companyId);
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
}
