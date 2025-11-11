package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.approval.entity.ApprovalDocument;
import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.constant.RequestUnit;
import com.crewvy.workforce_service.attendance.dto.request.DeviceRequestCreateDto;
import com.crewvy.workforce_service.attendance.dto.request.LeaveRequestCreateDto;
import com.crewvy.workforce_service.attendance.dto.request.TripRequestCreateDto;
import com.crewvy.workforce_service.attendance.dto.response.DeviceRequestResponse;
import com.crewvy.workforce_service.attendance.dto.response.LeaveRequestResponse;
import com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.TripRuleDto;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.Request;
import com.crewvy.workforce_service.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final com.crewvy.workforce_service.approval.repository.ApprovalDocumentRepository approvalDocumentRepository;
    private final CompanyHolidayRepository companyHolidayRepository;
    private final com.crewvy.workforce_service.salary.repository.HolidayRepository holidayRepository;

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

        // 0. 추가근무 신청 자동 분류 (방식 A: 기간 + 1일 연장시간)
        if (createDto.getDailyOvertimeHours() != null) {
            createDto = classifyAndPrepareExtraWorkRequest(memberId, memberPositionId, companyId, createDto);
        }

        // 1. 정책 조회 및 검증
        Policy policy = policyRepository.findById(createDto.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("정책을 찾을 수 없습니다."));

        // 1-1. 정책 활성화 상태 및 유효기간 검증
        validatePolicyStatus(policy);

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
                // 주말/공휴일 제외한 실제 근무일 계산
                deductionDays = (double) calculateWorkingDays(createDto.getStartAt(), createDto.getEndAt(), companyId);
                log.info("종일 휴가 계산: 시작일={}, 종료일={}, 실제 근무일={}일 (회사ID={})",
                        createDto.getStartAt(), createDto.getEndAt(), deductionDays, companyId);
                break;
            case HALF_DAY_AM:
                // 오전 반차는 신청일의 00:00 부터 12:00 까지로 설정
                startDateTime = createDto.getStartAt().atStartOfDay();
                endDateTime = createDto.getStartAt().atTime(12, 0, 0);
                // 주말/공휴일이면 차감하지 않음
                deductionDays = isNonWorkingDay(createDto.getStartAt(), companyId) ? 0.0 : 0.5;
                break;
            case HALF_DAY_PM:
                // 오후 반차는 신청일의 13:00 부터 23:59 까지로 설정 (점심시간 1시간 제외)
                startDateTime = createDto.getStartAt().atTime(13, 0, 0);
                endDateTime = createDto.getStartAt().atTime(23, 59, 59);
                // 주말/공휴일이면 차감하지 않음
                deductionDays = isNonWorkingDay(createDto.getStartAt(), companyId) ? 0.0 : 0.5;
                break;
            case TIME_OFF:
                startDateTime = createDto.getStartDateTime();
                endDateTime = createDto.getEndDateTime();
                long minutesBetween = ChronoUnit.MINUTES.between(startDateTime, endDateTime);

                // 정책에서 표준 근무 시간 조회 (fixedWorkMinutes)
                Integer standardWorkMinutes = getStandardWorkMinutesFromPolicy(policy);

                // 휴게 시간 조회 및 차감
                Integer breakMinutes = getBreakMinutesFromPolicy(policy);
                Integer actualWorkMinutes = standardWorkMinutes - (breakMinutes != null ? breakMinutes : 0);

                // 실제 근무시간 기준으로 차감 일수 계산
                deductionDays = Math.round((minutesBetween / (double) actualWorkMinutes) * 100) / 100.0;

                log.info("시차 휴가 계산: 신청시간={}분, 표준근무={}분, 휴게={}분, 실제근무={}분, 차감일수={}일",
                        minutesBetween, standardWorkMinutes, breakMinutes, actualWorkMinutes, deductionDays);
                break;
            default:
                throw new BusinessException("지원하지 않는 신청 단위입니다.");
        }


        // 4. 잔여 일수 확인 (balanceDeductible인 경우에만)
        if (policy.getPolicyTypeCode().isBalanceDeductible()) {
            validateMemberBalance(memberId, companyId, policy.getPolicyTypeCode(), deductionDays);
        }

        // 4-1. 연장/야간/휴일근무 정책 할당 여부 및 유형별 검증
        PolicyTypeCode typeCode = policy.getPolicyTypeCode();
        if (typeCode == PolicyTypeCode.OVERTIME
                || typeCode == PolicyTypeCode.NIGHT_WORK
                || typeCode == PolicyTypeCode.HOLIDAY_WORK) {
            // 해당 타입의 전용 정책이 할당되어 있는지 확인 (정책 할당 = 허용)
            validateOvertimePolicyAssigned(memberId, memberPositionId, companyId, typeCode);

            // 유형별 검증
            if (typeCode == PolicyTypeCode.OVERTIME) {
                // 연장근무: 주간 720분 한도 검증
                validateWeeklyOvertimeLimit(memberId, startDateTime, endDateTime);
            } else if (typeCode == PolicyTypeCode.NIGHT_WORK) {
                // 야간근무: 22:00~06:00 시간대 검증
                validateNightWorkTimeRange(startDateTime, endDateTime);
            } else if (typeCode == PolicyTypeCode.HOLIDAY_WORK) {
                // 휴일근무: 휴일(주말/공휴일) 여부 검증
                validateHolidayWork(companyId, startDateTime.toLocalDate(), endDateTime.toLocalDate());
            }
        }

        // 4-2. 중복 신청 확인 (동시성 제어)
        boolean isDuplicate = requestRepository.existsDuplicateRequest(
                memberId, policy.getId(), startDateTime, endDateTime);
        if (isDuplicate) {
            throw new BusinessException("동일한 기간에 이미 신청한 내역이 있습니다. 중복 신청은 불가능합니다.");
        }

        // 5. 자동 승인 여부에 따라 ApprovalDocument 연결 결정
        boolean isAutoApprove = Boolean.TRUE.equals(policy.getAutoApprove());
        ApprovalDocument approvalDocument = null;
        RequestStatus initialStatus = RequestStatus.PENDING;

        if (isAutoApprove) {
            // 자동 승인: 결재 문서 없이 바로 APPROVED 상태로 생성
            initialStatus = RequestStatus.APPROVED;
            log.info("자동 승인 정책 적용: memberId={}, policyId={}", memberId, policy.getId());
        } else {
            // 수동 승인: ApprovalDocument 연결 (결재 플로우 진행)
            approvalDocument = getApprovalDocumentByPolicyType(policy.getPolicyTypeCode());
        }

        // 6. Request 엔티티 생성
        Request request = Request.builder()
                .policy(policy)
                .memberId(memberId)
                .approvalDocument(approvalDocument)  // autoApprove=true면 null
                .requestUnit(createDto.getRequestUnit())
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .deductionDays(deductionDays)
                .reason(createDto.getReason())
                .status(initialStatus)  // autoApprove=true면 APPROVED, false면 PENDING
                .requesterComment(createDto.getRequesterComment())
                .workLocation(createDto.getWorkLocation()) // 출장지 (출장 신청 시 사용)
                .build();

        Request savedRequest = requestRepository.save(request);

        // 잔액 차감이 필요한 정책인 경우 즉시 차감 처리 (휴가)
        if (policy.getPolicyTypeCode().isBalanceDeductible()) {
            applyLeaveRequestBalance(savedRequest);
        }

        // 자동 승인된 경우: DailyAttendance 즉시 반영
        if (isAutoApprove) {
            // 연장/야간/휴일근무인 경우 근무시간 처리
            if (policy.getPolicyTypeCode() == PolicyTypeCode.OVERTIME
                    || policy.getPolicyTypeCode() == PolicyTypeCode.NIGHT_WORK
                    || policy.getPolicyTypeCode() == PolicyTypeCode.HOLIDAY_WORK) {
                applyWorkRequestToDailyAttendance(savedRequest);
            }
            log.info("자동 승인 처리 완료: memberId={}, policyId={}, requestId={}",
                    memberId, policy.getId(), savedRequest.getId());
        }

        log.info("휴가 신청 완료: memberId={}, policyId={}, deductionDays={}, autoApproved={}",
                memberId, policy.getId(), deductionDays, isAutoApprove);

        return LeaveRequestResponse.from(savedRequest);
    }

    /**
     * 출장 신청 생성
     * Phase 1: Request 생성 및 자동 승인 처리
     * Phase 2: Approval 생성 및 연동 추가 예정
     */
    public LeaveRequestResponse createTripRequest(
            UUID memberId,
            UUID memberPositionId,
            UUID companyId,
            UUID organizationId,
            TripRequestCreateDto createDto) {

        // 1. 정책 조회 및 검증
        Policy policy = policyRepository.findById(createDto.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("정책을 찾을 수 없습니다."));

        // 1-1. 정책 활성화 상태 및 유효기간 검증
        validatePolicyStatus(policy);

        // 2. 출장 정책 타입 검증
        if (policy.getPolicyTypeCode() != PolicyTypeCode.BUSINESS_TRIP) {
            throw new BusinessException("출장 정책이 아닙니다.");
        }

        // 3. 출장 기간 검증
        if (createDto.getStartAt().isAfter(createDto.getEndAt())) {
            throw new BusinessException("시작일은 종료일보다 이후일 수 없습니다.");
        }

        // 4. DateTime 계산 (출장은 일자 단위로만 신청)
        LocalDateTime startDateTime = createDto.getStartAt().atStartOfDay();
        LocalDateTime endDateTime = createDto.getEndAt().atTime(23, 59, 59);

        // 4-1. 중복 신청 확인 (동시성 제어)
        boolean isDuplicate = requestRepository.existsDuplicateRequest(
                memberId, policy.getId(), startDateTime, endDateTime);
        if (isDuplicate) {
            throw new BusinessException("동일한 기간에 이미 신청한 내역이 있습니다. 중복 신청은 불가능합니다.");
        }

        // 4-2. 출장지 검증 (정책에 허용된 출장지인지 확인)
        if (policy.getRuleDetails() != null && policy.getRuleDetails().getTripRule() != null) {
            TripRuleDto tripRule = policy.getRuleDetails().getTripRule();
            if (tripRule.getAllowedWorkLocations() != null && !tripRule.getAllowedWorkLocations().isEmpty()) {
                if (createDto.getWorkLocation() == null || !tripRule.getAllowedWorkLocations().contains(createDto.getWorkLocation())) {
                    throw new BusinessException("이 정책에서 허용되지 않는 출장지입니다. 허용된 출장지: " +
                            String.join(", ", tripRule.getAllowedWorkLocations()));
                }
            }
        }

        // 5. 자동 승인 여부에 따라 ApprovalDocument 연결 결정
        boolean isAutoApprove = Boolean.TRUE.equals(policy.getAutoApprove());
        ApprovalDocument approvalDocument = null;
        RequestStatus initialStatus = RequestStatus.PENDING;

        if (isAutoApprove) {
            // 자동 승인: 결재 문서 없이 바로 APPROVED 상태로 생성
            initialStatus = RequestStatus.APPROVED;
            log.info("자동 승인 정책 적용: memberId={}, policyId={}", memberId, policy.getId());
        } else {
            // 수동 승인: ApprovalDocument 연결 (결재 플로우 진행)
            approvalDocument = getApprovalDocumentByPolicyType(policy.getPolicyTypeCode());
        }

        // 6. Request 엔티티 생성
        Request request = Request.builder()
                .policy(policy)
                .memberId(memberId)
                .approvalDocument(approvalDocument)  // autoApprove=true면 null
                .requestUnit(RequestUnit.DAY) // 출장은 일자 단위
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .deductionDays(0.0) // 출장은 잔액 차감 없음
                .reason(createDto.getReason())
                .status(initialStatus)  // autoApprove=true면 APPROVED, false면 PENDING
                .requesterComment(createDto.getRequesterComment())
                .workLocation(createDto.getWorkLocation()) // 출장지
                .build();

        Request savedRequest = requestRepository.save(request);

        // 자동 승인된 경우 로그
        if (isAutoApprove) {
            log.info("출장 자동 승인 처리 완료: memberId={}, policyId={}, requestId={}",
                    memberId, policy.getId(), savedRequest.getId());
        }

        log.info("출장 신청 완료: memberId={}, policyId={}, workLocation={}, autoApproved={}",
                memberId, policy.getId(), createDto.getWorkLocation(), Boolean.TRUE.equals(policy.getAutoApprove()));

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

        // 신청 시 차감한 잔액 복구
        restoreBalanceAfterRejection(request);

        request.updateStatus(RequestStatus.CANCELED);

        // TODO: Phase 2 - Approval도 함께 취소 처리
        // if (request.getDocumentId() != null) {
        //     approvalService.cancelApproval(request.getDocumentId());
        // }
    }

    // --- 검증 메서드들 ---

    /**
     * 정책 활성화 상태 및 유효기간 검증
     */
    private void validatePolicyStatus(Policy policy) {
        // 1. 정책 활성화 상태 확인
        if (policy.getIsActive() == null || !policy.getIsActive()) {
            throw new BusinessException("비활성화된 정책입니다. 관리자에게 문의하세요.");
        }

        LocalDate today = LocalDate.now();

        // 2. 정책 시작일 확인
        if (policy.getEffectiveFrom() != null && today.isBefore(policy.getEffectiveFrom())) {
            throw new BusinessException(
                    String.format("정책 적용 시작일 이전입니다. (적용 시작일: %s)", policy.getEffectiveFrom())
            );
        }

        // 3. 정책 종료일 확인
        if (policy.getEffectiveTo() != null && today.isAfter(policy.getEffectiveTo())) {
            throw new BusinessException(
                    String.format("정책 적용 기간이 종료되었습니다. (종료일: %s)", policy.getEffectiveTo())
            );
        }
    }

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

        // 2. [신규] 공휴일 신청 제한 검증 (휴일근무 외 다른 신청은 공휴일에 불가)
        PolicyTypeCode policyTypeCode = policy.getPolicyTypeCode();
        boolean isWorkRelatedRequest = (policyTypeCode == PolicyTypeCode.HOLIDAY_WORK
                                        || policyTypeCode == PolicyTypeCode.OVERTIME
                                        || policyTypeCode == PolicyTypeCode.NIGHT_WORK);

        if (!isWorkRelatedRequest) {
            // 반차/시차는 해당 날짜가 비근무일이면 신청 불가
            if (createDto.getRequestUnit() == RequestUnit.HALF_DAY_AM
                || createDto.getRequestUnit() == RequestUnit.HALF_DAY_PM
                || createDto.getRequestUnit() == RequestUnit.TIME_OFF) {

                LocalDate targetDate = (createDto.getRequestUnit() == RequestUnit.TIME_OFF)
                    ? createDto.getStartDateTime().toLocalDate()
                    : createDto.getStartAt();

                if (isNonWorkingDay(targetDate, companyId)) {
                    String dayType = isWeekend(targetDate) ? "주말" : "공휴일";
                    throw new BusinessException(String.format("%s(%s)에는 휴가를 신청할 수 없습니다.",
                        dayType, targetDate.toString()));
                }
            }
            // 종일 휴가는 기간 내 모든 날짜가 비근무일이면 신청 불가
            else if (createDto.getRequestUnit() == RequestUnit.DAY) {
                long workingDays = calculateWorkingDays(requestStartDate, requestEndDate, companyId);
                if (workingDays == 0) {
                    throw new BusinessException(String.format("선택한 기간(%s ~ %s)은 모두 비근무일(주말/공휴일)입니다. 휴가를 신청할 수 없습니다.",
                        requestStartDate.toString(), requestEndDate.toString()));
                }
            }
        }

        // 3. [신규] 허용된 신청 단위 검증 (allowedRequestUnits)
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

        // 5-1. [신규] 최대 분할 횟수 확인 (maxSplitCount)
        if (leaveRule.getMaxSplitCount() != null) {
            int currentSplitCount = countApprovedRequestsByPolicy(memberId, policy.getId());
            if (currentSplitCount >= leaveRule.getMaxSplitCount()) {
                throw new BusinessException(
                    String.format("이 휴가는 최대 %d회까지 분할 사용 가능합니다. (현재 %d회 사용)",
                        leaveRule.getMaxSplitCount(), currentSplitCount)
                );
            }
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

    /**
     * 해당 정책으로 승인된 신청 횟수 조회 (현재 연도 기준)
     * 분할 사용 횟수 체크를 위해 사용됩니다.
     */
    private int countApprovedRequestsByPolicy(UUID memberId, UUID policyId) {
        LocalDate now = LocalDate.now();
        LocalDateTime yearStart = now.withDayOfYear(1).atStartOfDay();
        LocalDateTime yearEnd = now.withDayOfYear(now.lengthOfYear()).atTime(23, 59, 59);

        return requestRepository.countByMemberIdAndPolicyIdAndStatusAndStartDateTimeBetween(
                memberId,
                policyId,
                RequestStatus.APPROVED,
                yearStart,
                yearEnd
        );
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
        if (request.getPolicy() == null || !request.getPolicy().getPolicyTypeCode().isBalanceDeductible()) {
            return; // 차감 불필요한 정책은 스킵
        }

        int currentYear = LocalDate.now().getYear();
        PolicyTypeCode typeCode = request.getPolicy().getPolicyTypeCode();

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
                request.getPolicy().getPolicyTypeCode(),
                request.getRequestUnit()
        );

        if (leaveStatus == null) {
            log.warn("PolicyTypeCode를 AttendanceStatus로 매핑할 수 없습니다: {}",
                    request.getPolicy().getPolicyTypeCode());
            return;
        }

        List<DailyAttendance> attendances = dailyAttendanceRepository.findAllByMemberIdInAndAttendanceDateBetween(
                List.of(request.getMemberId()), startDate, effectiveEndDate);
        
        Map<LocalDate, DailyAttendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(DailyAttendance::getAttendanceDate, java.util.function.Function.identity()));

        List<DailyAttendance> attendancesToUpdate = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(effectiveEndDate)) {
            DailyAttendance dailyAttendance = attendanceMap.get(currentDate);
            if (dailyAttendance != null && dailyAttendance.getStatus() == AttendanceStatus.ABSENT) {
                dailyAttendance.updateStatus(leaveStatus);
                attendancesToUpdate.add(dailyAttendance);
                log.info("결근을 휴가로 변경: memberId={}, date={}, {} -> {}",
                        request.getMemberId(), currentDate, AttendanceStatus.ABSENT, leaveStatus);
            }
            currentDate = currentDate.plusDays(1);
        }
        if (!attendancesToUpdate.isEmpty()) {
            dailyAttendanceRepository.saveAll(attendancesToUpdate);
        }
    }

    /**
     * 연장/야간/휴일근무 신청 승인 시 처리
     * - DailyAttendance 생성/조회
     * - 근무시간 추가
     */
    private void applyWorkRequestToDailyAttendance(Request request) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = request.getStartDateTime().toLocalDate();
        LocalDate endDate = request.getEndDateTime().toLocalDate();

        // 사후 신청이 아니면 스킵
        if (startDate.isAfter(today)) {
            return;
        }

        LocalDate effectiveEndDate = endDate.isAfter(today) ? today : endDate;
        PolicyTypeCode typeCode = request.getPolicy().getPolicyTypeCode();

        List<DailyAttendance> attendances = dailyAttendanceRepository.findAllByMemberIdInAndAttendanceDateBetween(
                List.of(request.getMemberId()), startDate, effectiveEndDate);

        Map<LocalDate, DailyAttendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(DailyAttendance::getAttendanceDate, java.util.function.Function.identity()));

        List<DailyAttendance> attendancesToSave = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(effectiveEndDate)) {
            final LocalDate dateToProcess = currentDate;

            DailyAttendance dailyAttendance = attendanceMap.get(dateToProcess);

            if (dailyAttendance == null) {
                dailyAttendance = DailyAttendance.builder()
                        .memberId(request.getMemberId())
                        .companyId(request.getPolicy().getCompanyId())
                        .attendanceDate(dateToProcess)
                        .status(AttendanceStatus.NORMAL_WORK)
                        .build();
                attendancesToSave.add(dailyAttendance);
            }

            // 정책 타입에 따라 근무 시간 추가
            if (typeCode == PolicyTypeCode.OVERTIME) {
                // 연장근무 신청: 시간대별 자동 분리 (22:00 기준)
                OvertimeSplit split = splitOvertimeByTimeRange(request.getStartDateTime(), request.getEndDateTime());

                if (split.overtimeMinutes > 0) {
                    dailyAttendance.addOvertimeMinutes(split.overtimeMinutes);
                    dailyAttendance.addDaytimeOvertimeMinutes(split.overtimeMinutes);
                    log.info("사후 연장근무 반영: memberId={}, date={}, 연장={}분",
                            request.getMemberId(), dateToProcess, split.overtimeMinutes);
                }

                if (split.nightWorkMinutes > 0) {
                    dailyAttendance.addNightWorkMinutes(split.nightWorkMinutes);
                    log.info("사후 야간근무 반영 (연장근무 신청에서 분리): memberId={}, date={}, 야간={}분",
                            request.getMemberId(), dateToProcess, split.nightWorkMinutes);
                }

            } else if (typeCode == PolicyTypeCode.NIGHT_WORK) {
                long workMinutes = java.time.Duration.between(
                        request.getStartDateTime(),
                        request.getEndDateTime()
                ).toMinutes();
                dailyAttendance.addNightWorkMinutes((int) workMinutes);
                log.info("사후 야간근무 반영: memberId={}, date={}, minutes={}",
                        request.getMemberId(), dateToProcess, (int) workMinutes);

            } else if (typeCode == PolicyTypeCode.HOLIDAY_WORK) {
                long workMinutes = java.time.Duration.between(
                        request.getStartDateTime(),
                        request.getEndDateTime()
                ).toMinutes();
                dailyAttendance.addHolidayWorkMinutes((int) workMinutes);
                log.info("사후 휴일근무 반영: memberId={}, date={}, minutes={}",
                        request.getMemberId(), dateToProcess, (int) workMinutes);
            }

            if (!attendancesToSave.contains(dailyAttendance)) {
                attendancesToSave.add(dailyAttendance);
            }

            currentDate = currentDate.plusDays(1);
        }

        if (!attendancesToSave.isEmpty()) {
            dailyAttendanceRepository.saveAll(attendancesToSave);
        }
    }

    /**
     * 연장근무 시간대 자동 분리
     * - 22:00 이전: 연장근무 (overtimeMinutes)
     * - 22:00~06:00: 야간근무 (nightWorkMinutes)
     *
     * @param startDateTime 근무 시작 시간
     * @param endDateTime 근무 종료 시간
     * @return 분리된 연장/야간 근무 시간 (분 단위)
     */
    private OvertimeSplit splitOvertimeByTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        final LocalTime NIGHT_START = LocalTime.of(22, 0); // 야간근무 시작 22:00
        final LocalTime NIGHT_END = LocalTime.of(6, 0);    // 야간근무 종료 06:00

        int overtimeMinutes = 0;
        int nightWorkMinutes = 0;

        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        // 당일 22:00 시점
        LocalDateTime nightStartToday = LocalDateTime.of(startDate, NIGHT_START);
        // 다음날 06:00 시점
        LocalDateTime nightEndNextDay = LocalDateTime.of(startDate.plusDays(1), NIGHT_END);

        // 케이스 1: 종료 시간이 22:00 이전 → 모두 연장근무
        if (endDateTime.isBefore(nightStartToday) || endDateTime.equals(nightStartToday)) {
            overtimeMinutes = (int) java.time.Duration.between(startDateTime, endDateTime).toMinutes();
        }
        // 케이스 2: 시작 시간이 22:00 이후 또는 같음 → 모두 야간근무
        else if (!startDateTime.isBefore(nightStartToday)) {
            nightWorkMinutes = (int) java.time.Duration.between(startDateTime, endDateTime).toMinutes();
        }
        // 케이스 3: 시작은 22:00 이전, 종료는 22:00 이후 → 분리 필요
        else {
            // 22:00 이전 부분 → 연장근무
            overtimeMinutes = (int) java.time.Duration.between(startDateTime, nightStartToday).toMinutes();
            // 22:00 이후 부분 → 야간근무
            nightWorkMinutes = (int) java.time.Duration.between(nightStartToday, endDateTime).toMinutes();
        }

        return new OvertimeSplit(overtimeMinutes, nightWorkMinutes);
    }

    /**
     * 연장/야간 근무 시간 분리 결과
     */
    private static class OvertimeSplit {
        final int overtimeMinutes;
        final int nightWorkMinutes;

        OvertimeSplit(int overtimeMinutes, int nightWorkMinutes) {
            this.overtimeMinutes = overtimeMinutes;
            this.nightWorkMinutes = nightWorkMinutes;
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
            case BUSINESS_TRIP -> AttendanceStatus.BUSINESS_TRIP;
            default -> null; // 근무 시간 관련 정책은 휴가로 매핑하지 않음
        };
    }

    // === 승인 처리 메서드 (승인 서비스 연동 전 준비) ===

    /**
     * 신청 승인 처리 (휴가/연장/야간/휴일근무 모두 처리)
     */
    public void approveRequest(UUID requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("신청 내역을 찾을 수 없습니다."));

        validateApprovalStatus(request);

        PolicyTypeCode typeCode = request.getPolicy().getPolicyTypeCode();

        // 휴가 신청 승인 처리
        if (request.getPolicy().getPolicyTypeCode().isBalanceDeductible()) {
            // 잔액은 Request 생성 시 이미 차감됨 (PENDING 상태에서도)
            // 승인 시에는 DailyAttendance만 생성하여 근태 현황에 표시
            createFutureLeaveDailyAttendance(request);
        }
        // 연장/야간/휴일근무 처리
        else if (typeCode == PolicyTypeCode.OVERTIME
                || typeCode == PolicyTypeCode.NIGHT_WORK
                || typeCode == PolicyTypeCode.HOLIDAY_WORK) {
            applyWorkRequestToDailyAttendance(request);
        }
        // 출장 처리
        else if (typeCode == PolicyTypeCode.BUSINESS_TRIP) {
            // 출장은 DailyAttendance 생성만
            createFutureLeaveDailyAttendance(request);
        }

        request.updateStatus(RequestStatus.APPROVED);
        log.info("신청 승인 완료: requestId={}, memberId={}, type={}", 
                requestId, request.getMemberId(), typeCode);
    }
    
    /**
     * 휴가/연차 신청 승인 처리 (하위 호환성 유지)
     * @deprecated approveRequest() 사용 권장
     */
    @Deprecated
    public void approveLeaveRequest(UUID requestId) {
        approveRequest(requestId);
    }

    /**
     * 신청 거절 처리
     */
    public void rejectRequest(UUID requestId, String rejectReason) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("신청 내역을 찾을 수 없습니다."));

        validateRejectionStatus(request);

        // 신청 시 차감한 잔액 복구 (PENDING 상태에서 반려)
        // DailyAttendance는 PENDING → REJECTED 플로우에서는 생성되지 않으므로 복구 불필요
        restoreBalanceAfterRejection(request);

        request.updateStatus(RequestStatus.REJECTED);
        log.info("신청 거절 완료: requestId={}, memberId={}", requestId, request.getMemberId());
    }

    /**
     * 미래 휴가 DailyAttendance 생성
     */
    private void createFutureLeaveDailyAttendance(Request request) {
        LocalDate startDate = request.getStartDateTime().toLocalDate();
        LocalDate endDate = request.getEndDateTime().toLocalDate();

        AttendanceStatus leaveStatus = mapPolicyTypeToAttendanceStatus(
                request.getPolicy().getPolicyTypeCode(),
                request.getRequestUnit()
        );

        if (leaveStatus == null) {
            return;
        }

        List<DailyAttendance> attendances = dailyAttendanceRepository.findAllByMemberIdInAndAttendanceDateBetween(
                List.of(request.getMemberId()), startDate, endDate);
        
        Map<LocalDate, DailyAttendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(DailyAttendance::getAttendanceDate, java.util.function.Function.identity()));

        List<DailyAttendance> attendancesToSave = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DailyAttendance dailyAttendance = attendanceMap.get(currentDate);

            if (dailyAttendance == null) {
                dailyAttendance = DailyAttendance.builder()
                        .memberId(request.getMemberId())
                        .companyId(request.getPolicy().getCompanyId())
                        .attendanceDate(currentDate)
                        .status(leaveStatus)
                        .build();
            } else {
                dailyAttendance.updateStatus(leaveStatus);
            }
            attendancesToSave.add(dailyAttendance);
            currentDate = currentDate.plusDays(1);
        }
        
        if (!attendancesToSave.isEmpty()) {
            dailyAttendanceRepository.saveAll(attendancesToSave);
        }
    }

    /**
     * 거절 시 잔액 복구
     */
    private void restoreBalanceAfterRejection(Request request) {
        if (request.getPolicy() == null || !request.getPolicy().getPolicyTypeCode().isBalanceDeductible()) {
            return;
        }

        // deductionDays가 null이거나 0이면 복구할 필요 없음
        if (request.getDeductionDays() == null || request.getDeductionDays() == 0) {
            return;
        }

        int currentYear = LocalDate.now().getYear();
        PolicyTypeCode typeCode = request.getPolicy().getPolicyTypeCode();

        MemberBalance balance = memberBalanceRepository
                .findByMemberIdAndBalanceTypeCodeAndYear(request.getMemberId(), typeCode, currentYear)
                .orElse(null);

        if (balance == null) {
            return;
        }

        double newUsed = Math.max(0, balance.getTotalUsed() - request.getDeductionDays());
        double newRemaining = Math.min(balance.getTotalGranted(), balance.getRemaining() + request.getDeductionDays());

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
        log.info("잔액 복구 완료: memberId={}, restored={}", request.getMemberId(), request.getDeductionDays());
    }

    /**
     * 거절 시 DailyAttendance 복원
     */
    private void restoreDailyAttendanceAfterRejection(Request request) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = request.getStartDateTime().toLocalDate();
        LocalDate endDate = request.getEndDateTime().toLocalDate();
        LocalDate effectiveEndDate = endDate.isAfter(today) ? today : endDate;
        
        PolicyTypeCode typeCode = request.getPolicy().getPolicyTypeCode();

        List<DailyAttendance> attendances = dailyAttendanceRepository.findAllByMemberIdInAndAttendanceDateBetween(
                List.of(request.getMemberId()), startDate, effectiveEndDate);
        
        Map<LocalDate, DailyAttendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(DailyAttendance::getAttendanceDate, java.util.function.Function.identity()));

        List<DailyAttendance> attendancesToSave = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(effectiveEndDate)) {
            final LocalDate dateToProcess = currentDate;
            DailyAttendance da = attendanceMap.get(dateToProcess);
            if (da != null) {
                // 휴가 신청 복원: 휴가 상태 → 결근
                if (request.getPolicy().getPolicyTypeCode().isBalanceDeductible()
                    || typeCode == PolicyTypeCode.BUSINESS_TRIP) {
                    AttendanceStatus status = da.getStatus();
                    if (status == AttendanceStatus.ANNUAL_LEAVE
                        || status == AttendanceStatus.MATERNITY_LEAVE
                        || status == AttendanceStatus.PATERNITY_LEAVE
                        || status == AttendanceStatus.CHILDCARE_LEAVE
                        || status == AttendanceStatus.FAMILY_CARE_LEAVE
                        || status == AttendanceStatus.MENSTRUAL_LEAVE
                        || status == AttendanceStatus.HALF_DAY_AM
                        || status == AttendanceStatus.HALF_DAY_PM
                        || status == AttendanceStatus.BUSINESS_TRIP) {
                        da.updateStatus(AttendanceStatus.ABSENT);
                        attendancesToSave.add(da);
                    }
                }
                // 연장/야간/휴일근무 복원: 추가된 시간 차감
                else if (typeCode == PolicyTypeCode.OVERTIME
                        || typeCode == PolicyTypeCode.NIGHT_WORK
                        || typeCode == PolicyTypeCode.HOLIDAY_WORK) {
                    long workMinutes = java.time.Duration.between(
                            request.getStartDateTime(),
                            request.getEndDateTime()
                    ).toMinutes();
                    int dailyMinutes = (int) workMinutes;

                    // 신청 거부 시 해당 근무 시간을 DailyAttendance에서 차감
                    if (typeCode == PolicyTypeCode.OVERTIME && da.getOvertimeMinutes() != null && da.getOvertimeMinutes() > 0) {
                        da.subtractOvertimeMinutes(dailyMinutes);
                        attendancesToSave.add(da);
                        log.info("연장근무 복원 완료: memberId={}, date={}, 차감분={}분, 남은 연장근무={}분",
                                request.getMemberId(), dateToProcess, dailyMinutes, da.getOvertimeMinutes());
                    } else if (typeCode == PolicyTypeCode.NIGHT_WORK && da.getNightWorkMinutes() != null && da.getNightWorkMinutes() > 0) {
                        da.subtractNightWorkMinutes(dailyMinutes);
                        attendancesToSave.add(da);
                        log.info("야간근무 복원 완료: memberId={}, date={}, 차감분={}분, 남은 야간근무={}분",
                                request.getMemberId(), dateToProcess, dailyMinutes, da.getNightWorkMinutes());
                    } else if (typeCode == PolicyTypeCode.HOLIDAY_WORK && da.getHolidayWorkMinutes() != null && da.getHolidayWorkMinutes() > 0) {
                        da.subtractHolidayWorkMinutes(dailyMinutes);
                        attendancesToSave.add(da);
                        log.info("휴일근무 복원 완료: memberId={}, date={}, 차감분={}분, 남은 휴일근무={}분",
                                request.getMemberId(), dateToProcess, dailyMinutes, da.getHolidayWorkMinutes());
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        
        if (!attendancesToSave.isEmpty()) {
            dailyAttendanceRepository.saveAll(attendancesToSave);
        }
    }
    private void validateApprovalStatus(Request request) {
        if (request.getStatus() == RequestStatus.APPROVED) {
            throw new BusinessException("이미 승인된 신청입니다.");
        }
        if (request.getStatus() == RequestStatus.REJECTED) {
            throw new BusinessException("거절된 신청은 승인할 수 없습니다.");
        }
        if (request.getStatus() == RequestStatus.CANCELED) {
            throw new BusinessException("취소된 신청은 승인할 수 없습니다.");
        }
    }

    /**
     * 거절 가능 상태 검증
     */
    private void validateRejectionStatus(Request request) {
        if (request.getStatus() == RequestStatus.REJECTED) {
            throw new BusinessException("이미 거절된 신청입니다.");
        }
        if (request.getStatus() == RequestStatus.CANCELED) {
            throw new BusinessException("취소된 신청은 거절할 수 없습니다.");
        }
    }

    /**
     * 주간 연장근무 한도 검증 (근로기준법 제53조)
     * - 연장근무는 1주일에 12시간(720분)을 초과할 수 없음
     * - 위반 시 500만원 이하 과태료
     */
    private void validateWeeklyOvertimeLimit(UUID memberId, LocalDateTime requestStartTime, LocalDateTime requestEndTime) {
        // 1. 신청하려는 연장근무 시간 계산 (분 단위)
        LocalDate startDate = requestStartTime.toLocalDate();
        LocalDate endDate = requestEndTime.toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1; // 시작일 포함

        long totalDurationMinutes = java.time.Duration.between(requestStartTime, requestEndTime).toMinutes();
        // 여러 날에 걸친 경우: 날짜당 평균 연장시간 계산 후 주간에 해당하는 날짜만 합산
        long dailyOvertimeMinutes = daysBetween > 1 ? totalDurationMinutes / daysBetween : totalDurationMinutes;

        log.debug("연장근무 시간 계산: 기간={}~{}, 날짜수={}일, 총시간={}분, 1일평균={}분",
                startDate, endDate, daysBetween, totalDurationMinutes, dailyOvertimeMinutes);

        // 2. 이번 주 범위 계산 (월요일 00:00 ~ 일요일 23:59:59)
        LocalDate requestDate = requestStartTime.toLocalDate();
        LocalDate weekStart = requestDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);

        // 3. 이번 주에 이미 승인된/대기 중인 연장근무 신청 조회 (연장근무만 - 야간/휴일 제외)
        java.util.List<PolicyTypeCode> overtimeTypes = java.util.List.of(
                PolicyTypeCode.OVERTIME
        );
        java.util.List<Request> existingRequests = requestRepository.findApprovedOvertimeRequestsInWeek(
                memberId,
                weekStartDateTime,
                weekEndDateTime,
                overtimeTypes
        );

        // 4. 기존 연장근무 총 시간 계산 (날짜 수 고려)
        long totalExistingMinutes = existingRequests.stream()
                .mapToLong(r -> {
                    LocalDate rStartDate = r.getStartDateTime().toLocalDate();
                    LocalDate rEndDate = r.getEndDateTime().toLocalDate();
                    long rDays = ChronoUnit.DAYS.between(rStartDate, rEndDate) + 1;
                    long rTotalMinutes = java.time.Duration.between(r.getStartDateTime(), r.getEndDateTime()).toMinutes();
                    // 여러 날에 걸친 경우 1일 평균 계산
                    long rDailyMinutes = rDays > 1 ? rTotalMinutes / rDays : rTotalMinutes;

                    // 주간 범위 내에 있는 날짜만 카운트
                    long daysInWeek = 0;
                    for (LocalDate d = rStartDate; !d.isAfter(rEndDate); d = d.plusDays(1)) {
                        if (!d.isBefore(weekStart) && !d.isAfter(weekEnd)) {
                            daysInWeek++;
                        }
                    }
                    return rDailyMinutes * daysInWeek;
                })
                .sum();

        // 5. 신청하려는 시간 계산 (주간 범위 내 날짜만)
        long requestMinutes = 0;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            if (!d.isBefore(weekStart) && !d.isAfter(weekEnd)) {
                requestMinutes += dailyOvertimeMinutes;
            }
        }

        // 6. 신청하려는 시간 + 기존 시간이 720분(12시간)을 초과하는지 확인
        long totalMinutes = totalExistingMinutes + requestMinutes;
        final int WEEKLY_OVERTIME_LIMIT_MINUTES = 720; // 근로기준법 제53조: 주 12시간

        if (totalMinutes > WEEKLY_OVERTIME_LIMIT_MINUTES) {
            double totalHours = totalMinutes / 60.0;
            double existingHours = totalExistingMinutes / 60.0;
            double requestHours = requestMinutes / 60.0;

            throw new BusinessException(
                    String.format("근로기준법 제53조 위반: 주간 연장근무는 12시간을 초과할 수 없습니다.\n" +
                            "- 이번 주 기존 연장근무: %.1f시간\n" +
                            "- 신청하려는 시간: %.1f시간\n" +
                            "- 총 합계: %.1f시간 (한도: 12시간)",
                            existingHours, requestHours, totalHours)
            );
        }

        log.info("주간 연장근무 한도 검증 통과: memberId={}, 기간={}~{} ({}일), 1일평균={}분, 주간신청={}분, 기존={}분, 총={}분 (한도: 720분)",
                memberId, startDate, endDate, daysBetween, dailyOvertimeMinutes, requestMinutes, totalExistingMinutes, totalMinutes);
    }

    /**
     * 연장/야간/휴일근무 정책 할당 여부 검증
     * 해당 타입의 전용 정책이 할당되어 있는지 확인 (정책 할당 = 허용)
     *
     * @param memberId 직원 ID
     * @param memberPositionId 직원 직책 ID
     * @param companyId 회사 ID
     * @param typeCode 신청하려는 근무 타입 (OVERTIME, NIGHT_WORK, HOLIDAY_WORK)
     */
    private void validateOvertimePolicyAssigned(UUID memberId, UUID memberPositionId, UUID companyId, PolicyTypeCode typeCode) {
        // 해당 타입의 전용 정책이 할당되어 있는지 확인
        Policy assignedPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, typeCode);

        if (assignedPolicy == null) {
            String policyName = switch (typeCode) {
                case OVERTIME -> "연장근무";
                case NIGHT_WORK -> "야간근무";
                case HOLIDAY_WORK -> "휴일근무";
                default -> "해당";
            };
            throw new BusinessException(policyName + " 정책이 할당되지 않았습니다. 관리자에게 문의하세요.");
        }

        log.info("{} 정책 할당 확인 완료: policyId={}", typeCode, assignedPolicy.getId());
    }

    /**
     * 야간근무 시간대 검증
     * - 야간근무는 22:00~06:00 시간대에만 가능
     */
    private void validateNightWorkTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LocalTime startTime = startDateTime.toLocalTime();
        LocalTime endTime = endDateTime.toLocalTime();

        // 22:00~06:00 범위 검증
        boolean isValidStart = startTime.isAfter(LocalTime.of(21, 59)) || startTime.isBefore(LocalTime.of(6, 0));
        boolean isValidEnd = endTime.isAfter(LocalTime.of(22, 0)) || endTime.isBefore(LocalTime.of(6, 1));

        if (!isValidStart || !isValidEnd) {
            throw new BusinessException("야간근무는 22:00~06:00 시간대에만 신청 가능합니다. (신청 시간: "
                + startTime + "~" + endTime + ")");
        }

        log.info("야간근무 시간대 검증 완료: {}~{}", startTime, endTime);
    }

    /**
     * 휴일근무 검증
     * - 휴일근무는 주말 또는 공휴일에만 가능
     */
    private void validateHolidayWork(UUID companyId, LocalDate startDate, LocalDate endDate) {
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (!isHoliday(companyId, currentDate)) {
                throw new BusinessException("휴일근무는 주말 또는 공휴일에만 신청 가능합니다. (" + currentDate + "는 평일입니다)");
            }
            currentDate = currentDate.plusDays(1);
        }

        log.info("휴일근무 검증 완료: {}~{}", startDate, endDate);
    }

    /**
     * 휴일 여부 확인 (주말 또는 CompanyHoliday)
     */
    private boolean isHoliday(UUID companyId, LocalDate date) {
        // 1. 주말(토요일, 일요일) 확인
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true;
        }

        // 2. 국가 공휴일 확인 (Holidays 테이블)
        if (holidayRepository.existsBySolarDate(date)) {
            return true;
        }

        // 3. CompanyHoliday 확인
        return companyHolidayRepository.existsByCompanyIdAndHolidayDate(companyId, date);
    }

    /**
     * 정책에서 표준 근무 시간(분) 조회
     */
    private Integer getStandardWorkMinutesFromPolicy(Policy policy) {
        if (policy == null || policy.getRuleDetails() == null || policy.getRuleDetails().getWorkTimeRule() == null) {
            log.warn("정책에 WorkTimeRule이 없어 기본값 480분을 사용합니다.");
            return 480; // 기본값: 8시간
        }

        Integer fixedWorkMinutes = policy.getRuleDetails().getWorkTimeRule().getFixedWorkMinutes();
        if (fixedWorkMinutes == null || fixedWorkMinutes <= 0) {
            log.warn("정책의 fixedWorkMinutes가 유효하지 않아 기본값 480분을 사용합니다.");
            return 480;
        }

        return fixedWorkMinutes;
    }

    /**
     * 정책에서 휴게 시간(분) 조회
     * - FIXED 모드: fixedBreakStart ~ fixedBreakEnd 시간 차이
     * - AUTO 모드: defaultBreakMinutesFor8Hours
     * - MANUAL 모드: 0 (사용자가 직접 기록)
     */
    private Integer getBreakMinutesFromPolicy(Policy policy) {
        if (policy == null || policy.getRuleDetails() == null || policy.getRuleDetails().getBreakRule() == null) {
            log.debug("정책에 BreakRule이 없어 휴게시간 0으로 처리합니다.");
            return 0;
        }

        com.crewvy.workforce_service.attendance.dto.rule.BreakRuleDto breakRule = policy.getRuleDetails().getBreakRule();
        String breakType = breakRule.getType();

        if ("FIXED".equals(breakType)) {
            // FIXED: 고정 휴게 시간 계산
            if (breakRule.getFixedBreakStart() != null && breakRule.getFixedBreakEnd() != null) {
                try {
                    String[] startParts = breakRule.getFixedBreakStart().split(":");
                    String[] endParts = breakRule.getFixedBreakEnd().split(":");
                    int startMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
                    int endMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);
                    return endMinutes - startMinutes;
                } catch (Exception e) {
                    log.error("FIXED 휴게시간 파싱 실패", e);
                    return 0;
                }
            }
        } else if ("AUTO".equals(breakType)) {
            // AUTO: 정책에 설정된 기본 휴게시간
            Integer defaultBreakMinutes = breakRule.getDefaultBreakMinutesFor8Hours();
            return defaultBreakMinutes != null ? defaultBreakMinutes : 60; // 기본 60분
        } else if ("MANUAL".equals(breakType)) {
            // MANUAL: 휴게시간은 사용자가 직접 기록하므로 0
            return 0;
        }

        log.debug("알 수 없는 휴게 규칙 타입: {}", breakType);
        return 0;
    }

    /**
     * 정책 타입에 따라 ApprovalDocument를 자동으로 매핑합니다.
     * @param policyTypeCode 정책 타입 코드
     * @return 매핑된 ApprovalDocument
     */
    private com.crewvy.workforce_service.approval.entity.ApprovalDocument getApprovalDocumentByPolicyType(PolicyTypeCode policyTypeCode) {
        String documentName = switch(policyTypeCode) {
            case ANNUAL_LEAVE, MATERNITY_LEAVE, PATERNITY_LEAVE, MENSTRUAL_LEAVE, FAMILY_CARE_LEAVE -> "휴가 신청서";
            case CHILDCARE_LEAVE -> "휴직 신청서";
            case BUSINESS_TRIP -> "출장 신청서";
            case OVERTIME, NIGHT_WORK, HOLIDAY_WORK -> "추가근무 신청서";
            default -> throw new BusinessException("해당 정책 타입에 대한 결재 문서가 정의되지 않았습니다: " + policyTypeCode);
        };

        return approvalDocumentRepository.findByDocumentName(documentName)
                .orElseThrow(() -> new ResourceNotFoundException("결재 문서를 찾을 수 없습니다: " + documentName));
    }

    /**
     * 추가근무 신청 자동 분류 (방식 A: 기간 + 1일 연장시간)
     * - 기간과 1일 연장시간을 받아서 휴일/야간/연장근무를 자동 판단
     * - 적절한 정책을 선택하고 startDateTime, endDateTime 계산
     */
    private LeaveRequestCreateDto classifyAndPrepareExtraWorkRequest(
            UUID memberId,
            UUID memberPositionId,
            UUID companyId,
            LeaveRequestCreateDto createDto) {

        // 1. 신청자의 유효한 STANDARD_WORK 정책 조회 (workEndTime 필요)
        Policy standardWorkPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, PolicyTypeCode.STANDARD_WORK);

        if (standardWorkPolicy == null || standardWorkPolicy.getRuleDetails() == null
                || standardWorkPolicy.getRuleDetails().getWorkTimeRule() == null) {
            throw new BusinessException("기본 근무 정책을 찾을 수 없습니다. 추가근무 신청을 진행할 수 없습니다.");
        }

        String workEndTimeStr = standardWorkPolicy.getRuleDetails().getWorkTimeRule().getWorkEndTime();
        if (workEndTimeStr == null || workEndTimeStr.isBlank()) {
            throw new BusinessException("기본 근무 정책에 퇴근 시간이 설정되어 있지 않습니다.");
        }

        LocalTime workEndTime = LocalTime.parse(workEndTimeStr); // "HH:mm" → LocalTime

        // 2. dailyOvertimeHours 파싱 (HH:mm)
        String[] parts = createDto.getDailyOvertimeHours().split(":");
        if (parts.length != 2) {
            throw new BusinessException("1일 연장시간 형식이 올바르지 않습니다. (예: 02:00)");
        }
        int overtimeHours = Integer.parseInt(parts[0]);
        int overtimeMinutes = Integer.parseInt(parts[1]);
        LocalTime overtimeDuration = LocalTime.of(overtimeHours, overtimeMinutes);

        // 3. 기간 내 각 날짜 확인 (휴일 여부, 야간 여부)
        LocalDate startDate = createDto.getStartAt();
        LocalDate endDate = createDto.getEndAt();
        boolean hasHoliday = false;
        boolean hasNightWork = false;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // 3-1. 휴일 여부 확인 (토/일요일 체크 - 공휴일은 추후 추가)
            if (date.getDayOfWeek().getValue() >= 6) { // 토요일(6), 일요일(7)
                hasHoliday = true;
            }

            // 3-2. 야간 여부 확인 (22:00~06:00)
            LocalTime overtimeEndTime = workEndTime.plusHours(overtimeHours).plusMinutes(overtimeMinutes);
            if (overtimeEndTime.isAfter(LocalTime.of(22, 0)) || overtimeEndTime.isBefore(LocalTime.of(6, 0))) {
                hasNightWork = true;
            }
        }

        // 4. 자동 분류: 휴일 > 야간 > 연장 순서
        PolicyTypeCode selectedPolicyType;
        if (hasHoliday) {
            selectedPolicyType = PolicyTypeCode.HOLIDAY_WORK;
        } else if (hasNightWork) {
            selectedPolicyType = PolicyTypeCode.NIGHT_WORK;
        } else {
            selectedPolicyType = PolicyTypeCode.OVERTIME;
        }

        // 5. 해당 정책 조회
        Policy selectedPolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId, memberPositionId, companyId, selectedPolicyType);

        if (selectedPolicy == null) {
            throw new BusinessException(selectedPolicyType.getCodeName() + " 정책을 찾을 수 없습니다.");
        }

        // 6. startDateTime, endDateTime 계산
        // 첫 날: startDate + workEndTime
        // 마지막 날: endDate + workEndTime + overtime
        LocalDateTime startDateTime = startDate.atTime(workEndTime);
        LocalDateTime endDateTime = endDate.atTime(workEndTime.plusHours(overtimeHours).plusMinutes(overtimeMinutes));

        // 7. 수정된 createDto 반환 (Builder 패턴 사용)
        return LeaveRequestCreateDto.builder()
                .policyId(selectedPolicy.getId())
                .requestUnit(RequestUnit.TIME_OFF) // 추가근무는 시간 단위
                .startAt(createDto.getStartAt())
                .endAt(createDto.getEndAt())
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .dailyOvertimeHours(createDto.getDailyOvertimeHours())
                .reason(createDto.getReason())
                .requesterComment(createDto.getRequesterComment())
                .workLocation(createDto.getWorkLocation())
                .documentId(createDto.getDocumentId())
                .build();
    }

    /**
     * 주말(토요일, 일요일) 여부 확인
     */
    private boolean isWeekend(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * 비근무일(주말 또는 공휴일) 여부 확인
     * @param date 확인할 날짜
     * @param companyId 회사 ID
     * @return 비근무일이면 true
     */
    private boolean isNonWorkingDay(LocalDate date, UUID companyId) {
        // 1. 주말 체크
        if (isWeekend(date)) {
            return true;
        }
        // 2. 법정 공휴일 체크 (Holidays 테이블)
        if (holidayRepository.existsBySolarDate(date)) {
            return true;
        }
        // 3. 회사 지정 휴일 체크 (CompanyHoliday 테이블) - 추후 확장용
        // return companyHolidayRepository.existsByCompanyIdAndHolidayDate(companyId, date);
        return false;
    }

    /**
     * 주말/공휴일 제외한 실제 근무일 계산
     * @param startDate 시작일 (포함)
     * @param endDate 종료일 (포함)
     * @param companyId 회사 ID (공휴일 조회용)
     * @return 실제 근무일 수
     */
    private long calculateWorkingDays(LocalDate startDate, LocalDate endDate, UUID companyId) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("시작일은 종료일보다 이후일 수 없습니다.");
        }

        long workingDays = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // 주말도 아니고 공휴일도 아니면 근무일로 카운트
            if (!isNonWorkingDay(currentDate, companyId)) {
                workingDays++;
            }
            currentDate = currentDate.plusDays(1);
        }

        log.debug("근무일 계산: {}~{} = {}일 (회사ID={})", startDate, endDate, workingDays, companyId);
        return workingDays;
    }
}
