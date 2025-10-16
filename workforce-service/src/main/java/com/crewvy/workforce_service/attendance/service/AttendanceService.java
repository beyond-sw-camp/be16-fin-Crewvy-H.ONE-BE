package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.BusinessException;
import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.EventType;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.dto.request.EventRequest;
import com.crewvy.workforce_service.attendance.dto.response.ClockInResponse;
import com.crewvy.workforce_service.attendance.dto.response.ClockOutResponse;
import com.crewvy.workforce_service.attendance.dto.rule.AuthMethodDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.crewvy.workforce_service.attendance.entity.AttendanceLog;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.repository.AttendanceLogRepository;
import com.crewvy.workforce_service.attendance.repository.DailyAttendanceRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.RequestRepository;
import com.crewvy.workforce_service.attendance.util.DistanceCalculator;
import com.crewvy.workforce_service.feignClient.MemberClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final RequestRepository requestRepository;
    private final PolicyRepository policyRepository;
    private final MemberClient memberClient;

    public ApiResponse<?> recordEvent(UUID memberId, UUID memberPositionId, UUID companyId, EventRequest request, String clientIp) {
        checkPermissionOrThrow(memberPositionId, "CREATE", "INDIVIDUAL", "근태를 기록할 권한이 없습니다.");

        // 인증/검증이 필요한 이벤트 그룹
        List<EventType> validationRequiredEvents = List.of(EventType.CLOCK_IN, EventType.CLOCK_OUT);

        if (validationRequiredEvents.contains(request.getEventType())) {
            validate(memberId, companyId, request.getDeviceId(), request.getDeviceType(), request.getLatitude(), request.getLongitude(), clientIp);
        }

        switch (request.getEventType()) {
            case CLOCK_IN:
                ClockInResponse clockInResponse = clockIn(memberId, request);
                return ApiResponse.success(clockInResponse, "출근 등록 완료.");
            case CLOCK_OUT:
                ClockOutResponse clockOutResponse = clockOut(memberId, request);
                return ApiResponse.success(clockOutResponse, "퇴근 등록 완료.");
            default:
                throw new BusinessException("지원하지 않는 이벤트 타입입니다.");
        }
    }

    private void checkPermissionOrThrow(UUID memberPositionId, String action, String range, String errorMessage) {
        ApiResponse<Boolean> response = memberClient.checkPermission(memberPositionId, "attendance", action, range);
        if (response == null || !Boolean.TRUE.equals(response.getData())) {
            throw new BusinessException(errorMessage);
        }
    }

    private ClockInResponse clockIn(UUID memberId, EventRequest request) {
        LocalDate today = LocalDate.now();
        dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .ifPresent(d -> {
                    throw new BusinessException("이미 출근 처리되었습니다.");
                });

        LocalDateTime clockInTime = LocalDateTime.now();
        AttendanceLog newLog = createAttendanceLog(memberId, clockInTime, EventType.CLOCK_IN, request.getLatitude(), request.getLongitude());
        createDailyAttendance(memberId, today, clockInTime);

        return new ClockInResponse(newLog.getAttendanceLogId(), newLog.getEventTime());
    }

    private ClockOutResponse clockOut(UUID memberId, EventRequest request) {

        LocalDateTime clockOutTime = LocalDateTime.now();
        AttendanceLog newLog = createAttendanceLog(memberId, clockOutTime, EventType.CLOCK_OUT, request.getLatitude(), request.getLongitude());

        LocalDate today = LocalDate.now();
        DailyAttendance dailyAttendance = dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, today)
                .orElseThrow(() -> new BusinessException("출근 기록이 없습니다. 퇴근 처리할 수 없습니다."));

        dailyAttendance.updateClockOut(clockOutTime);

        return new ClockOutResponse(
                newLog.getAttendanceLogId(),
                newLog.getEventTime(),
                dailyAttendance.getWorkedMinutes(),
                dailyAttendance.getOvertimeMinutes()
        );
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

    private void createDailyAttendance(UUID memberId, LocalDate today, LocalDateTime clockInTime) {
        DailyAttendance dailyAttendance = DailyAttendance.builder()
                .memberId(memberId)
                .attendanceDate(today)
                .firstClockIn(clockInTime)
                .workedMinutes(0)
                .overtimeMinutes(0)
                .totalBreakMinutes(0)
                .build();
        dailyAttendanceRepository.save(dailyAttendance);
    }

    // --- 이하 검증(validate) 관련 헬퍼 메서드들 ---
    private void validate(UUID memberId, UUID companyId, String deviceId, DeviceType deviceType, Double latitude, Double longitude, String clientIp) {
        validateApprovedDevice(deviceId, memberId, deviceType);
        Policy activePolicy = findActivePolicy(companyId);
        PolicyRuleDetails ruleDetails = activePolicy.getRuleDetails();
        validateAuthRule(ruleDetails, deviceType, latitude, longitude, clientIp);
    }

    private void validateApprovedDevice(String deviceId, UUID memberId, DeviceType deviceType) {
        if (deviceId == null) {
            throw new BusinessException("디바이스 ID가 없습니다.");
        }
        boolean isApproved = requestRepository.existsApprovedDevice(
                memberId,
                deviceId,
                deviceType,
                RequestStatus.APPROVED
        );
        if (!isApproved) {
            throw new BusinessException("등록되지 않았거나 미승인된 디바이스입니다.");
        }
    }

    private Policy findActivePolicy(UUID companyId) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Policy> policyPage = policyRepository.findActivePolicies(companyId, LocalDate.now(), pageable);
        if (policyPage.isEmpty()) {
            throw new BusinessException("적용된 출퇴근 정책이 없습니다.");
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
                .orElseThrow(() -> new BusinessException("현재 기기에서 지원하는 인증 방식이 정책에 없습니다."));

        String authMethod = applicableMethod.getAuthMethod();
        Map<String, Object> details = applicableMethod.getDetails();

        switch (authMethod) {
            case "GPS":
                if (latitude == null || longitude == null) {
                    throw new BusinessException("GPS 인증 방식에는 좌표 정보가 필수입니다.");
                }
                validateGpsLocation(details, latitude, longitude);
                break;
            case "NETWORK_IP":
                validateIpAddress(details, clientIp);
                break;
            default:
                throw new BusinessException("정책에 알 수 없는 인증 방식이 설정되어 있습니다.");
        }
    }

    private void validateGpsLocation(Map<String, Object> rules, double userLat, double userLon) {
        double gpsRadius = ((Number) rules.get("gpsRadiusMeters")).doubleValue();
        double officeLat = ((Number) rules.get("officeLatitude")).doubleValue();
        double officeLon = ((Number) rules.get("officeLongitude")).doubleValue();
        double distance = DistanceCalculator.calculateDistanceInMeters(officeLat, officeLon, userLat, userLon);
        if (distance > gpsRadius) {
            throw new BusinessException(String.format("지정된 근무지로부터 약 %.0f미터 벗어났습니다.", distance));
        }
    }

    private void validateIpAddress(Map<String, Object> rules, String clientIp) {
        List<String> allowedIps = (List<String>) rules.get("allowedIps");
        if (allowedIps == null || allowedIps.isEmpty()) {
            throw new BusinessException("정책에 허용된 IP가 등록되지 않았습니다.");
        }
        if (!allowedIps.contains(clientIp)) {
            throw new BusinessException("허용되지 않은 IP입니다.");
        }
    }
}
