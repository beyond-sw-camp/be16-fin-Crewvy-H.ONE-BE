package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.constant.RequestUnit;
import com.crewvy.workforce_service.attendance.dto.request.DeviceRequestCreateDto;
import com.crewvy.workforce_service.attendance.dto.request.LeaveRequestCreateDto;
import com.crewvy.workforce_service.attendance.dto.response.DeviceRequestResponse;
import com.crewvy.workforce_service.attendance.dto.response.LeaveRequestResponse;
import com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.Request;
import com.crewvy.workforce_service.attendance.repository.DailyAttendanceRepository;
import com.crewvy.workforce_service.attendance.repository.MemberBalanceRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RequestService {

    private final RequestRepository requestRepository;
    private final PolicyRepository policyRepository;
    private final MemberBalanceRepository memberBalanceRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final PolicyAssignmentService policyAssignmentService;
    // private final ApprovalService approvalService; // TODO: Phase 2 - Approval 연동

    /**
     * 휴가 신청 생성
     * Phase 1: Request 생성 및 검증만 수행
     * Phase 2: Approval 생성 및 연동 추가 예정
     */
    public LeaveRequestResponse createLeaveRequest(
            UUID memberId,
            UUID memberPositionId,
            UUID companyId,
            UUID organizationId,
            LeaveRequestCreateDto createDto) {

        // 1. 정책 조회 및 검증
        Policy policy = policyRepository.findById(createDto.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("정책을 찾을 수 없습니다."));

        // 2. 정책 규칙 및 요청 유효성 검증
        validateLeaveRequest(policy, createDto, memberId, companyId);

        // 3. 요청 단위를 기준으로 LocalDateTime 및 차감 일수 계산
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        Double deductionDays;

        switch (createDto.getRequestUnit()) {
            case DAY:
                startDateTime = createDto.getStartAt().atStartOfDay();
                endDateTime = createDto.getEndAt().atTime(23, 59, 59);
                deductionDays = (double) (ChronoUnit.DAYS.between(createDto.getStartAt(), createDto.getEndAt()) + 1);
                break;
            case HALF_DAY_AM:
                // 오전 반차는 신청일의 00:00 부터 12:00 까지로 설정
                startDateTime = createDto.getStartAt().atStartOfDay();
                endDateTime = createDto.getStartAt().atTime(12, 0, 0);
                deductionDays = 0.5;
                break;
            case HALF_DAY_PM:
                // 오후 반차는 신청일의 13:00 부터 23:59 까지로 설정 (점심시간 1시간 제외)
                startDateTime = createDto.getStartAt().atTime(13, 0, 0);
                endDateTime = createDto.getStartAt().atTime(23, 59, 59);
                deductionDays = 0.5;
                break;
            case TIME_OFF:
                startDateTime = createDto.getStartDateTime();
                endDateTime = createDto.getEndDateTime();
                long minutesBetween = ChronoUnit.MINUTES.between(startDateTime, endDateTime);
                // 하루 근무를 8시간(480분)으로 가정하여 차감 일수 계산
                deductionDays = Math.round((minutesBetween / 480.0) * 100) / 100.0;
                break;
            default:
                throw new BusinessException("지원하지 않는 신청 단위입니다.");
        }


        // 4. 잔여 일수 확인 (balanceDeductible인 경우에만)
        if (policy.getPolicyType().isBalanceDeductible()) {
            validateMemberBalance(memberId, companyId, policy.getPolicyType().getTypeCode(), deductionDays);
        }

        // 5. Request 엔티티 생성
        Request request = Request.builder()
                .policy(policy)
                .memberId(memberId)
                .documentId(null) // TODO: Phase 2 - Approval 생성 후 업데이트
                .requestUnit(createDto.getRequestUnit())
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .deductionDays(deductionDays)
                .reason(createDto.getReason())
                .status(RequestStatus.PENDING)
                .requesterComment(createDto.getRequesterComment())
                .workLocation(createDto.getWorkLocation()) // 출장지 (출장 신청 시 사용)
                .build();

        Request savedRequest = requestRepository.save(request);

        // TODO: Phase 2 - Approval 생성 및 연동
        // UUID approvalId = approvalService.createApproval(...);
        // savedRequest.updateDocumentId(approvalId);

        // 자동 승인 처리
        if (Boolean.TRUE.equals(policy.getAutoApprove())) {
            // 잔액 차감이 필요한 정책인 경우 차감 처리
            if (policy.getPolicyType().isBalanceDeductible()) {
                applyLeaveRequestBalance(savedRequest);
            }
            savedRequest.updateStatus(RequestStatus.APPROVED);
            log.info("자동 승인 처리 완료: memberId={}, policyId={}, requestId={}",
                    memberId, policy.getId(), savedRequest.getId());
        }

        log.info("휴가 신청 완료: memberId={}, policyId={}, deductionDays={}, autoApproved={}",
                memberId, policy.getId(), deductionDays, Boolean.TRUE.equals(policy.getAutoApprove()));

        return LeaveRequestResponse.from(savedRequest);
    }

    /**
     * 내 모든 신청 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getMyRequests(UUID memberId, Pageable pageable) {
        Page<Request> requests = requestRepository.findByMemberId(memberId, pageable);
        return requests.map(LeaveRequestResponse::from);
    }

    /**
     * 내 휴가 신청 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getMyLeaveRequests(UUID memberId, Pageable pageable) {
        Page<Request> requests = requestRepository.findLeaveRequestsByMemberId(memberId, pageable);
        return requests.map(LeaveRequestResponse::from);
    }

    /**
     * 휴가 신청 상세 조회
     */
    @Transactional(readOnly = true)
    public LeaveRequestResponse getRequestById(UUID requestId, UUID memberId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("신청 내역을 찾을 수 없습니다."));

        // 본인 신청만 조회 가능
        if (!request.getMemberId().equals(memberId)) {
            throw new BusinessException("본인의 신청 내역만 조회할 수 있습니다.");
        }

        return LeaveRequestResponse.from(request);
    }

    /**
     * 휴가 신청 취소
     */
    public void cancelRequest(UUID requestId, UUID memberId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("신청 내역을 찾을 수 없습니다."));

        // 본인 신청만 취소 가능
        if (!request.getMemberId().equals(memberId)) {
            throw new BusinessException("본인의 신청만 취소할 수 있습니다.");
        }

        // PENDING 상태만 취소 가능
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("대기 중인 신청만 취소할 수 있습니다.");
        }

        request.updateStatus(RequestStatus.CANCELED);

        // TODO: Phase 2 - Approval도 함께 취소 처리
        // if (request.getDocumentId() != null) {
        //     approvalService.cancelApproval(request.getDocumentId());
        // }
    }

    // --- 검증 메서드들 ---

    /**
     * 휴가 신청 검증
     */
    private void validateLeaveRequest(Policy policy, LeaveRequestCreateDto createDto, UUID memberId, UUID companyId) {
        LeaveRuleDto leaveRule = policy.getRuleDetails().getLeaveRule();

        // 출장 정책 등 leaveRule이 없는 경우 기본 검증만 수행
        if (leaveRule == null) {
            // 기본 날짜/시간 유효성만 검증
            LocalDateTime startDateTime;
            LocalDateTime endDateTime;

            if (createDto.getRequestUnit() == RequestUnit.TIME_OFF) {
                if (createDto.getStartDateTime() == null || createDto.getEndDateTime() == null) {
                    throw new BusinessException("시차 신청 시에는 시작 및 종료 시각을 모두 입력해야 합니다.");
                }
                if (createDto.getStartDateTime().isAfter(createDto.getEndDateTime())) {
                    throw new BusinessException("시작 시각은 종료 시각보다 이후일 수 없습니다.");
                }
                startDateTime = createDto.getStartDateTime();
                endDateTime = createDto.getEndDateTime();
            } else {
                if (createDto.getStartAt() == null || createDto.getEndAt() == null) {
                    throw new BusinessException("일차 신청 시에는 시작일과 종료일을 모두 입력해야 합니다.");
                }
                if (createDto.getStartAt().isAfter(createDto.getEndAt())) {
                    throw new BusinessException("시작일은 종료일보다 이후일 수 없습니다.");
                }
                startDateTime = createDto.getStartAt().atStartOfDay();
                endDateTime = createDto.getEndAt().atTime(23, 59, 59);
            }

            // 중복 신청 확인
            validateDuplicateRequest(memberId, startDateTime, endDateTime);
            return; // leaveRule 상세 검증은 건너뜀
        }

        LocalDate requestStartDate;
        LocalDate requestEndDate;
        double requestedDays;

        // 1. 요청 단위별 날짜/시간 유효성 검증 및 기간 계산
        if (createDto.getRequestUnit() == RequestUnit.TIME_OFF) {
            if (createDto.getStartDateTime() == null || createDto.getEndDateTime() == null) {
                throw new BusinessException("시차 신청 시에는 시작 및 종료 시각을 모두 입력해야 합니다.");
            }
            if (!createDto.getStartDateTime().toLocalDate().equals(createDto.getEndDateTime().toLocalDate())) {
                throw new BusinessException("시차 신청은 같은 날짜 내에서만 가능합니다.");
            }
            if (createDto.getStartDateTime().isAfter(createDto.getEndDateTime())) {
                throw new BusinessException("시작 시각은 종료 시각보다 이후일 수 없습니다.");
            }
            requestStartDate = createDto.getStartDateTime().toLocalDate();
            requestEndDate = createDto.getEndDateTime().toLocalDate();
            long minutesBetween = ChronoUnit.MINUTES.between(createDto.getStartDateTime(), createDto.getEndDateTime());
            requestedDays = Math.round((minutesBetween / 480.0) * 100) / 100.0; // 8시간 기준 일수 변환
        } else {
            if (createDto.getStartAt() == null || createDto.getEndAt() == null) {
                throw new BusinessException("일차/반차 신청 시에는 시작일과 종료일을 모두 입력해야 합니다.");
            }
            if (createDto.getStartAt().isAfter(createDto.getEndAt())) {
                throw new BusinessException("시작일은 종료일보다 이후일 수 없습니다.");
            }
            requestStartDate = createDto.getStartAt();
            requestEndDate = createDto.getEndAt();
            requestedDays = (createDto.getRequestUnit() == RequestUnit.DAY) ? (ChronoUnit.DAYS.between(requestStartDate, requestEndDate) + 1) : 0.5;
        }

        // 2. [신규] 허용된 신청 단위 검증 (allowedRequestUnits)
        if (leaveRule.getAllowedRequestUnits() != null && !leaveRule.getAllowedRequestUnits().isEmpty()) {
            boolean isAllowed = leaveRule.getAllowedRequestUnits().stream()
                    .anyMatch(unit -> unit.equals(createDto.getRequestUnit().name()));
            if (!isAllowed) {
                throw new BusinessException(String.format("이 정책은 '%s' 단위로 신청할 수 없습니다. (허용 단위: %s)",
                        createDto.getRequestUnit().getCodeName(), String.join(", ", leaveRule.getAllowedRequestUnits())));
            }
        }

        // 3. 신청 시점 검증 (사전/사후 신청)
        LocalDate today = LocalDate.now();
        long daysFromStart = ChronoUnit.DAYS.between(requestStartDate, today);

        if (daysFromStart < 0) {
            // 사전 신청 (휴가 시작일이 미래)
            if (leaveRule.getRequestDeadlineDays() != null) {
                long daysUntilStart = -daysFromStart; // 양수로 변환
                if (daysUntilStart < leaveRule.getRequestDeadlineDays()) {
                    throw new BusinessException(
                            String.format("휴가 시작일 %d일 전까지 신청해야 합니다.", leaveRule.getRequestDeadlineDays())
                    );
                }
            }
        } else {
            // 사후 신청 (휴가 시작일이 현재 또는 과거)
            if (leaveRule.getAllowRetrospectiveRequest() == null || !leaveRule.getAllowRetrospectiveRequest()) {
                throw new BusinessException("이 휴가는 사전 신청만 가능합니다. 사후 신청이 허용되지 않습니다.");
            }

            if (leaveRule.getRetrospectiveRequestDays() != null && daysFromStart > leaveRule.getRetrospectiveRequestDays()) {
                throw new BusinessException(
                        String.format("사후 신청은 휴가 시작일로부터 %d일 이내에만 가능합니다. (현재 %d일 경과)",
                                leaveRule.getRetrospectiveRequestDays(), daysFromStart)
                );
            }
        }

        // 4. [신규] 1회 최대 신청 가능일수 확인 (maxDaysPerRequest)
        if (leaveRule.getMaxDaysPerRequest() != null && requestedDays > leaveRule.getMaxDaysPerRequest()) {
            throw new BusinessException(String.format("이 휴가는 한 번에 최대 %d일까지 신청할 수 있습니다.", leaveRule.getMaxDaysPerRequest()));
        }

        // 5. [신규] 최소 연속 신청일수 확인 (minConsecutiveDays)
        if (leaveRule.getMinConsecutiveDays() != null && requestedDays < leaveRule.getMinConsecutiveDays()) {
            throw new BusinessException(String.format("이 휴가는 최소 %d일 이상 연속으로 신청해야 합니다.", leaveRule.getMinConsecutiveDays()));
        }

        // 6. 기간별 최대 사용일수 확인 (limitPeriod, maxDaysPerPeriod)
        if (leaveRule.getLimitPeriod() != null && leaveRule.getMaxDaysPerPeriod() != null) {
            double totalUsedDays = calculateTotalUsedDaysInPeriod(memberId, policy.getId(), leaveRule.getLimitPeriod());
            if (totalUsedDays + requestedDays > leaveRule.getMaxDaysPerPeriod()) {
                throw new BusinessException(
                    String.format("이 휴가는 %s %d일까지 사용할 수 있습니다. (현재까지 %.1f일 사용)",
                        leaveRule.getLimitPeriod().equals("YEARLY") ? "연간" : "월간",
                        leaveRule.getMaxDaysPerPeriod(),
                        totalUsedDays)
                );
            }
        }

        // 7. 중복 신청 확인
        LocalDateTime startDateTimeForValidation = requestStartDate.atStartOfDay();
        LocalDateTime endDateTimeForValidation = requestEndDate.atTime(23, 59, 59);
        validateDuplicateRequest(memberId, startDateTimeForValidation, endDateTimeForValidation);
    }



    /**
     * 중복 신청 확인
     */
    private void validateDuplicateRequest(UUID memberId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        boolean hasDuplicate = requestRepository.existsByMemberIdAndDateRangeAndStatus(
                memberId, startDateTime, endDateTime, RequestStatus.PENDING
        );

        if (hasDuplicate) {
            throw new BusinessException("해당 기간에 이미 신청한 내역이 있습니다.");
        }
    }

    /**
     * 잔여 일수 확인
     */
    private void validateMemberBalance(UUID memberId, UUID companyId, PolicyTypeCode typeCode, Double deductionDays) {
        int currentYear = LocalDate.now().getYear();

        MemberBalance balance = memberBalanceRepository
                .findByMemberIdAndBalanceTypeCodeAndYear(memberId, typeCode, currentYear)
                .orElseThrow(() -> new BusinessException("잔여 일수 정보를 찾을 수 없습니다."));

        if (balance.getRemaining() < deductionDays) {
            throw new BusinessException(
                    String.format("잔여 일수가 부족합니다. (잔여: %.1f일, 신청: %.1f일)",
                            balance.getRemaining(), deductionDays)
            );
        }
    }

    /**
     * 특정 기간(연/월) 동안 특정 정책의 휴가를 얼마나 사용했는지 계산합니다.
     */
    private double calculateTotalUsedDaysInPeriod(UUID memberId, UUID policyId, String limitPeriod) {
        LocalDate now = LocalDate.now();
        LocalDateTime periodStart;
        LocalDateTime periodEnd;

        if ("YEARLY".equals(limitPeriod)) {
            periodStart = now.withDayOfYear(1).atStartOfDay();
            periodEnd = now.withDayOfYear(now.lengthOfYear()).atTime(23, 59, 59);
        } else if ("MONTHLY".equals(limitPeriod)) {
            periodStart = now.withDayOfMonth(1).atStartOfDay();
            periodEnd = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);
        } else {
            return 0.0; // 알 수 없는 기간 타입은 검증 통과
        }

        // 해당 기간에 승인된(APPROVED) 동일 정책의 휴가 사용일수를 합산
        return requestRepository.sumDeductionDaysByMemberIdAndPolicyIdAndStatusInDateRange(
                memberId,
                policyId,
                RequestStatus.APPROVED,
                periodStart,
                periodEnd
        ).orElse(0.0);
    }



    // TODO: Phase 2 - Approval 결재 완료 콜백
    // public void handleApprovalResult(UUID documentId, ApprovalState state) {
    //     Request request = requestRepository.findByDocumentId(documentId)
    //             .orElseThrow(() -> new ResourceNotFoundException("신청 내역을 찾을 수 없습니다."));
    //
    //     if (state == ApprovalState.APPROVED) {
    //         request.updateStatus(RequestStatus.APPROVED);
    //         // DailyAttendance 생성 또는 MemberBalance 차감
    //         applyApprovedRequest(request);
    //     } else if (state == ApprovalState.REJECTED) {
    //         request.updateStatus(RequestStatus.REJECTED);
    //     }
    // }

    // ==================== 디바이스 관리 ====================

    /**
     * 디바이스 등록 신청 (사용자)
     */
    public DeviceRequestResponse registerDevice(UUID memberId, DeviceRequestCreateDto createDto) {
        // 중복 등록 확인
        boolean alreadyExists = requestRepository.existsByMemberIdAndDeviceIdAndDeviceType(
                memberId, createDto.getDeviceId(), createDto.getDeviceType());

        if (alreadyExists) {
            throw new BusinessException("이미 등록된 디바이스입니다.");
        }

        // Request 엔티티 생성
        Request request = Request.builder()
                .memberId(memberId)
                .deviceId(createDto.getDeviceId())
                .deviceName(createDto.getDeviceName())
                .deviceType(createDto.getDeviceType())
                .reason(createDto.getReason())
                .status(RequestStatus.PENDING)
                .policy(null)  // 디바이스 등록은 정책이 없음
                .requestUnit(null)
                .startDateTime(null)
                .endDateTime(null)
                .deductionDays(null)
                .build();

        Request savedRequest = requestRepository.save(request);
        log.info("디바이스 등록 신청 완료: memberId={}, deviceId={}, deviceType={}",
                memberId, createDto.getDeviceId(), createDto.getDeviceType());

        return DeviceRequestResponse.from(savedRequest);
    }

    /**
     * 내 디바이스 등록 신청 목록 조회 (사용자)
     */
    @Transactional(readOnly = true)
    public Page<DeviceRequestResponse> getMyDeviceRequests(UUID memberId, Pageable pageable) {
        Page<Request> requests = requestRepository.findDeviceRequestsByMemberId(memberId, pageable);
        return requests.map(DeviceRequestResponse::from);
    }

    /**
     * 승인 대기 중인 디바이스 등록 신청 목록 (관리자)
     */
    @Transactional(readOnly = true)
    public Page<DeviceRequestResponse> getPendingDeviceRequests(Pageable pageable) {
        Page<Request> requests = requestRepository.findDeviceRequestsByStatus(RequestStatus.PENDING, pageable);
        return requests.map(DeviceRequestResponse::from);
    }

    /**
     * 디바이스 승인 (관리자)
     */
    public void approveDeviceRequest(UUID requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("디바이스 등록 신청을 찾을 수 없습니다."));

        if (request.getDeviceId() == null) {
            throw new BusinessException("디바이스 등록 신청이 아닙니다.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("대기 중인 신청만 승인할 수 있습니다.");
        }

        request.updateStatus(RequestStatus.APPROVED);
        log.info("디바이스 승인 완료: requestId={}, memberId={}, deviceId={}",
                requestId, request.getMemberId(), request.getDeviceId());
    }

    /**
     * 디바이스 반려 (관리자)
     */
    public void rejectDeviceRequest(UUID requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("디바이스 등록 신청을 찾을 수 없습니다."));

        if (request.getDeviceId() == null) {
            throw new BusinessException("디바이스 등록 신청이 아닙니다.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("대기 중인 신청만 반려할 수 있습니다.");
        }

        request.updateStatus(RequestStatus.REJECTED);
        log.info("디바이스 반려 완료: requestId={}, memberId={}, deviceId={}",
                requestId, request.getMemberId(), request.getDeviceId());
    }

    /**
     * 휴가 신청 승인 시 MemberBalance 차감 처리
     */
    private void applyLeaveRequestBalance(Request request) {
        if (request.getPolicy() == null || !request.getPolicy().getPolicyType().isBalanceDeductible()) {
            return; // 차감 불필요한 정책은 스킵
        }

        int currentYear = LocalDate.now().getYear();
        PolicyTypeCode typeCode = request.getPolicy().getPolicyType().getTypeCode();

        MemberBalance balance = memberBalanceRepository
                .findByMemberIdAndBalanceTypeCodeAndYear(request.getMemberId(), typeCode, currentYear)
                .orElseThrow(() -> new BusinessException("잔여 일수 정보를 찾을 수 없습니다."));

        // 잔액 차감
        double newUsed = balance.getTotalUsed() + request.getDeductionDays();
        double newRemaining = balance.getRemaining() - request.getDeductionDays();

        // MemberBalance 업데이트를 위해 새로운 엔티티 생성
        MemberBalance updatedBalance = MemberBalance.builder()
                .id(balance.getId())
                .memberId(balance.getMemberId())
                .companyId(balance.getCompanyId())
                .balanceTypeCode(balance.getBalanceTypeCode())
                .year(balance.getYear())
                .totalGranted(balance.getTotalGranted())
                .totalUsed(newUsed)
                .remaining(newRemaining)
                .expirationDate(balance.getExpirationDate())
                .isPaid(balance.getIsPaid())
                .build();

        memberBalanceRepository.save(updatedBalance);

        log.info("휴가 잔액 차감 완료: memberId={}, typeCode={}, deducted={}, remaining={}",
                request.getMemberId(), typeCode, request.getDeductionDays(), newRemaining);

        // 사후 신청 처리: 결근을 휴가로 변경
        updateAbsentToDaysToLeave(request);
    }

    /**
     * 사후 신청 시 결근 상태를 휴가로 변경
     */
    private void updateAbsentToDaysToLeave(Request request) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = request.getStartDateTime().toLocalDate();
        LocalDate endDate = request.getEndDateTime().toLocalDate();

        // 사후 신청이 아니면 스킵 (휴가 시작일이 미래)
        if (startDate.isAfter(today)) {
            return;
        }

        // 해당 기간의 DailyAttendance 조회 (오늘 이전 날짜만)
        LocalDate effectiveEndDate = endDate.isAfter(today) ? today : endDate;

        // PolicyTypeCode에 따른 AttendanceStatus 매핑
        AttendanceStatus leaveStatus = mapPolicyTypeToAttendanceStatus(
                request.getPolicy().getPolicyType().getTypeCode(),
                request.getRequestUnit()
        );

        if (leaveStatus == null) {
            log.warn("PolicyTypeCode를 AttendanceStatus로 매핑할 수 없습니다: {}",
                    request.getPolicy().getPolicyType().getTypeCode());
            return;
        }

        // 기간 내 모든 날짜의 DailyAttendance 조회 및 변경
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(effectiveEndDate)) {
            final LocalDate dateToProcess = currentDate;
            dailyAttendanceRepository.findByMemberIdAndAttendanceDate(request.getMemberId(), dateToProcess)
                    .ifPresent(dailyAttendance -> {
                        if (dailyAttendance.getStatus() == AttendanceStatus.ABSENT) {
                            dailyAttendance.updateStatus(leaveStatus);
                            dailyAttendanceRepository.save(dailyAttendance);
                            log.info("결근을 휴가로 변경: memberId={}, date={}, {} -> {}",
                                    request.getMemberId(), dateToProcess, AttendanceStatus.ABSENT, leaveStatus);
                        }
                    });
            currentDate = currentDate.plusDays(1);
        }
    }

    /**
     * PolicyTypeCode를 AttendanceStatus로 매핑
     */
    private AttendanceStatus mapPolicyTypeToAttendanceStatus(PolicyTypeCode typeCode, RequestUnit requestUnit) {
        // 반차는 RequestUnit에 따라 결정
        if (requestUnit == RequestUnit.HALF_DAY_AM) {
            return AttendanceStatus.HALF_DAY_AM;
        } else if (requestUnit == RequestUnit.HALF_DAY_PM) {
            return AttendanceStatus.HALF_DAY_PM;
        }

        // 종일 휴가는 PolicyTypeCode에 따라 결정
        return switch (typeCode) {
            case ANNUAL_LEAVE -> AttendanceStatus.ANNUAL_LEAVE;
            case MATERNITY_LEAVE -> AttendanceStatus.MATERNITY_LEAVE;
            case PATERNITY_LEAVE -> AttendanceStatus.PATERNITY_LEAVE;
            case CHILDCARE_LEAVE -> AttendanceStatus.CHILDCARE_LEAVE;
            case FAMILY_CARE_LEAVE -> AttendanceStatus.FAMILY_CARE_LEAVE;
            case MENSTRUAL_LEAVE -> AttendanceStatus.MENSTRUAL_LEAVE;
            default -> null; // 근무 시간 관련 정책은 휴가로 매핑하지 않음
        };
    }
}
