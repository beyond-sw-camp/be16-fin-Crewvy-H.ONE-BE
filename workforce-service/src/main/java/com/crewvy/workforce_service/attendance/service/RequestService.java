package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.constant.RequestUnit;
import com.crewvy.workforce_service.attendance.dto.request.DeviceRequestCreateDto;
import com.crewvy.workforce_service.attendance.dto.request.LeaveRequestCreateDto;
import com.crewvy.workforce_service.attendance.dto.response.DeviceRequestResponse;
import com.crewvy.workforce_service.attendance.dto.response.LeaveRequestResponse;
import com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.Request;
import com.crewvy.workforce_service.attendance.repository.MemberBalanceRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.RequestRepository;
// import com.crewvy.workforce_service.approval.service.ApprovalService; // TODO: Phase 2에서 주입
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RequestService {

    private final RequestRepository requestRepository;
    private final PolicyRepository policyRepository;
    private final MemberBalanceRepository memberBalanceRepository;
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
            LeaveRequestCreateDto dto) {

        // 1. 정책 조회 및 검증
        Policy policy = policyRepository.findById(dto.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("정책을 찾을 수 없습니다."));

        // 2. 정책 규칙 검증
        validateLeaveRequest(policy, dto, memberId, companyId);

        // 3. 차감 일수 계산
        Double deductionDays = calculateDeductionDays(dto.getStartAt(), dto.getEndAt(), dto.getRequestUnit());

        // 4. 잔여 일수 확인 (balanceDeductible인 경우에만)
        if (policy.getPolicyType().isBalanceDeductible()) {
            validateMemberBalance(memberId, companyId, policy.getPolicyType().getTypeCode(), deductionDays);
        }

        // 5. Request 엔티티 생성
        Request request = Request.builder()
                .policy(policy)
                .memberId(memberId)
                .documentId(null) // TODO: Phase 2 - Approval 생성 후 업데이트
                .requestUnit(dto.getRequestUnit())
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .deductionDays(deductionDays)
                .reason(dto.getReason())
                .status(RequestStatus.PENDING)
                .requesterComment(dto.getRequesterComment())
                .workLocation(dto.getWorkLocation()) // 출장지 (출장 신청 시 사용)
                .build();

        Request savedRequest = requestRepository.save(request);

        // TODO: Phase 2 - Approval 생성 및 연동
        // UUID approvalId = approvalService.createApproval(...);
        // savedRequest.updateDocumentId(approvalId);

        return LeaveRequestResponse.from(savedRequest);
    }

    /**
     * 내 휴가 신청 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getMyRequests(UUID memberId, Pageable pageable) {
        Page<Request> requests = requestRepository.findByMemberId(memberId, pageable);
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
    private void validateLeaveRequest(Policy policy, LeaveRequestCreateDto dto, UUID memberId, UUID companyId) {
        LeaveRuleDto leaveRule = policy.getRuleDetails().getLeaveRule();
        if (leaveRule == null) {
            throw new BusinessException("휴가 규칙이 설정되지 않은 정책입니다.");
        }

        // 1. 시작일 <= 종료일 확인
        if (dto.getStartAt().isAfter(dto.getEndAt())) {
            throw new BusinessException("시작일은 종료일보다 이후일 수 없습니다.");
        }

        // 2. 신청 마감일 확인 (requestDeadlineDays)
        if (leaveRule.getRequestDeadlineDays() != null) {
            long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(), dto.getStartAt());
            if (daysUntilStart < leaveRule.getRequestDeadlineDays()) {
                throw new BusinessException(
                        String.format("휴가 시작일 %d일 전까지 신청해야 합니다.", leaveRule.getRequestDeadlineDays())
                );
            }
        }

        // 3. 최소 신청 단위 확인 (minimumRequestUnit)
        if (leaveRule.getMinimumRequestUnit() != null) {
            validateRequestUnit(dto.getRequestUnit(), leaveRule.getMinimumRequestUnit());
        }

        // 4. 중복 신청 확인
        validateDuplicateRequest(memberId, dto.getStartAt(), dto.getEndAt());
    }

    /**
     * 신청 단위 검증
     */
    private void validateRequestUnit(RequestUnit requestUnit, String minimumUnit) {
        // minimumUnit: "DAY", "HALF_DAY", "HOUR"
        switch (minimumUnit) {
            case "DAY":
                if (requestUnit != RequestUnit.DAY) {
                    throw new BusinessException("이 정책은 일 단위로만 신청 가능합니다.");
                }
                break;
            case "HALF_DAY":
                // DAY, HALF_DAY_AM, HALF_DAY_PM 모두 허용
                break;
            case "HOUR":
                // 모든 단위 허용 (시간 단위 추가 시 구현)
                break;
        }
    }

    /**
     * 중복 신청 확인
     */
    private void validateDuplicateRequest(UUID memberId, LocalDate startAt, LocalDate endAt) {
        boolean hasDuplicate = requestRepository.existsByMemberIdAndDateRangeAndStatus(
                memberId, startAt, endAt, RequestStatus.PENDING
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
     * 차감 일수 계산
     */
    private Double calculateDeductionDays(LocalDate startAt, LocalDate endAt, RequestUnit requestUnit) {
        long daysBetween = ChronoUnit.DAYS.between(startAt, endAt) + 1; // 시작일 포함

        switch (requestUnit) {
            case DAY:
                return (double) daysBetween;
            case HALF_DAY_AM:
            case HALF_DAY_PM:
                return 0.5;
            default:
                throw new BusinessException("지원하지 않는 신청 단위입니다.");
        }
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
    public DeviceRequestResponse registerDevice(UUID memberId, DeviceRequestCreateDto dto) {
        // 중복 등록 확인
        boolean alreadyExists = requestRepository.existsByMemberIdAndDeviceIdAndDeviceType(
                memberId, dto.getDeviceId(), dto.getDeviceType());

        if (alreadyExists) {
            throw new BusinessException("이미 등록된 디바이스입니다.");
        }

        // Request 엔티티 생성
        Request request = Request.builder()
                .memberId(memberId)
                .deviceId(dto.getDeviceId())
                .deviceName(dto.getDeviceName())
                .deviceType(dto.getDeviceType())
                .reason(dto.getReason())
                .status(RequestStatus.PENDING)
                .policy(null)  // 디바이스 등록은 정책이 없음
                .requestUnit(null)
                .startAt(null)
                .endAt(null)
                .deductionDays(null)
                .build();

        Request savedRequest = requestRepository.save(request);
        log.info("디바이스 등록 신청 완료: memberId={}, deviceId={}, deviceType={}",
                memberId, dto.getDeviceId(), dto.getDeviceType());

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
}
