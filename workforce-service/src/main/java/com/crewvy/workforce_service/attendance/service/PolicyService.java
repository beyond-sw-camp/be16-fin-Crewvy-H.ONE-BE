package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.dto.request.PolicyCreateRequest;
import com.crewvy.workforce_service.attendance.dto.request.PolicyUpdateRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyResponse;
import com.crewvy.workforce_service.attendance.dto.response.PolicyTypeResponse;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyType;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyTypeRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final ObjectMapper objectMapper;
    private final MemberClient memberClient;

    public PolicyResponse createPolicy(UUID memberpositionId, UUID companyId, UUID organizationId, PolicyCreateRequest request) {
        checkPermissionOrThrow(memberpositionId, "CREATE", "COMPANY", "회사 정책 생성 권한이 없습니다.");

        PolicyType policyType = policyTypeRepository.findByCompanyIdAndTypeCode(companyId, request.getTypeCode())
                .orElseThrow(() -> new ResourceNotFoundException("해당 회사에 존재하지 않는 정책 유형입니다."));

        PolicyRuleDetails ruleDetails = convertAndValidateRuleDetails(request.getRuleDetails(), request.getTypeCode());

        Policy newPolicy = Policy.builder()
                .companyId(companyId)
                .policyType(policyType)
                .name(request.getName())
                .isPaid(request.getIsPaid())
                .isActive(false)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .ruleDetails(ruleDetails)
                .build();

        Policy savedPolicy = policyRepository.save(newPolicy);
        return new PolicyResponse(savedPolicy);
    }

    @Transactional(readOnly = true)
    public PolicyResponse findPolicyById(UUID memberpositionId, UUID companyId, UUID policyId) {
        checkPermissionOrThrow(memberpositionId, "READ", "COMPANY", "회사 정책 조회 권한이 없습니다.");

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("ID에 해당하는 정책을 찾을 수 없습니다: " + policyId));
        return new PolicyResponse(policy);
    }

    @Transactional(readOnly = true)
    public Page<PolicyResponse> findAllPoliciesByCompany(UUID memberpositionId, UUID companyId, Pageable pageable) {
        checkPermissionOrThrow(memberpositionId, "READ", "COMPANY", "회사 정책 조회 권한이 없습니다.");

        Page<Policy> policyPage = policyRepository.findByCompanyId(companyId, pageable);
        return policyPage.map(PolicyResponse::new);
    }

    public PolicyResponse updatePolicy(UUID memberpositionId, UUID companyId, UUID policyId, PolicyUpdateRequest request) {
        checkPermissionOrThrow(memberpositionId, "UPDATE", "COMPANY", "회사 정책 수정 권한이 없습니다.");
        // 1. 수정할 Policy 엔티티를 조회
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("ID에 해당하는 정책을 찾을 수 없습니다: " + policyId));

        // 2. PolicyType이 존재하는지 확인
        PolicyType policyType = policyTypeRepository.findByCompanyIdAndTypeCode(policy.getCompanyId(), request.getTypeCode())
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 정책 유형입니다."));

        // 3. ruleDetails 변환 및 검증
        PolicyRuleDetails ruleDetails = convertAndValidateRuleDetails(request.getRuleDetails(), policyType.getTypeCode());

        // 4. 엔티티 내용 업데이트
        policy.update(
                policyType,
                request.getName(),
                request.getIsPaid(),
                request.getEffectiveFrom(),
                request.getEffectiveTo(),
                ruleDetails
        );
        return new PolicyResponse(policy);
    }

    public void deletePolicy(UUID memberpositionId, UUID companyId, UUID policyId) {
        checkPermissionOrThrow(memberpositionId, "DELETE", "COMPANY", "회사 정책 삭제 권한이 없습니다.");

        if (!policyRepository.existsById(policyId)) {
            throw new BusinessException("ID에 해당하는 정책을 찾을 수 없습니다: " + policyId);
        }
        policyRepository.deleteById(policyId);
    }

    public void activatePolicies(UUID memberpositionId, UUID companyId, List<UUID> policyIds) {
        checkPermissionOrThrow(memberpositionId, "UPDATE", "COMPANY", "회사 정책 수정 권한이 없습니다.");

        List<Policy> policiesToActivate = policyRepository.findAllById(policyIds);
        if (policiesToActivate.size() != policyIds.size()) {
            throw new BusinessException("요청된 ID 목록에 존재하지 않는 정책이 포함되어 있습니다.");
        }
        // 요청된 모든 정책을 활성화 상태로 변경
        policiesToActivate.forEach(Policy::activate);
    }

    public void deactivatePolicies(UUID memberpositionId, UUID companyId, List<UUID> policyIds) {
        checkPermissionOrThrow(memberpositionId, "UPDATE", "COMPANY", "회사 정책 수정 권한이 없습니다.");

        List<Policy> policiesToDeactivate = policyRepository.findAllById(policyIds);
        if (policiesToDeactivate.size() != policyIds.size()) {
            throw new BusinessException("요청된 ID 목록에 존재하지 않는 정책이 포함되어 있습니다.");
        }
        policiesToDeactivate.forEach(Policy::deactivate);
    }

    @Transactional(readOnly = true)
    public List<PolicyTypeResponse> findPolicyTypesByCompany(UUID memberpositionId, UUID companyId) {

        checkPermissionOrThrow(memberpositionId, "READ", "COMPANY", "회사 정책 조회 권한이 없습니다.");

        return policyTypeRepository.findByCompanyId(companyId)
                .stream()
                .map(PolicyTypeResponse::new)
                .collect(Collectors.toList());
    }

    private void checkPermissionOrThrow(UUID memberPositionId, String action, String range, String errorMessage) {
        ApiResponse<Boolean> response = memberClient.checkPermission(memberPositionId, "attendance", action, range);
        if (response == null || !Boolean.TRUE.equals(response.getData())) {
            throw new PermissionDeniedException(errorMessage);
        }
    }

    private PolicyRuleDetails convertAndValidateRuleDetails(Map<String, Object> ruleDetailsMap, PolicyTypeCode typeCode) {
        if (ruleDetailsMap == null || ruleDetailsMap.isEmpty()) {
            if (!typeCode.isBalanceDeductible()) { // 근무 관련 정책일 경우
                throw new InvalidPolicyRuleException("근무 관련 정책에는 세부 규칙(ruleDetails)이 필수입니다.");
            }
            return null;
        }
        try {
            PolicyRuleDetails ruleDetails = objectMapper.convertValue(ruleDetailsMap, PolicyRuleDetails.class);

            // 1. isBalanceDeductible에 따른 '구조적' 유효성 검증
            validateStructureByDeductibility(ruleDetails, typeCode.isBalanceDeductible());

            // 2. 존재하는 규칙 블록에 대해서만 '내부' 유효성 검증을 선택적으로 실행
            if (ruleDetails.getAuthRule() != null) {
                validateAuthRuleDetails(ruleDetails.getAuthRule());
            }
            if (ruleDetails.getWorkTimeRule() != null) {
                validateWorkTimeRuleDetails(ruleDetails);
            }
            if (ruleDetails.getLeaveRule() != null) {
                validateLeaveRuleDetails(ruleDetails.getLeaveRule());
            }
            if (ruleDetails.getTripRule() != null) {
                validateTripRuleDetails(ruleDetails.getTripRule());
            }
            if (ruleDetails.getGoOutRule() != null) {
                validateGoOutRuleDetails(ruleDetails.getGoOutRule());
            }
            if (ruleDetails.getBreakRule() != null) {
                validateBreakRuleDetails(ruleDetails.getBreakRule());
            }
            if (ruleDetails.getLatenessRule() != null) {
                validateLatenessRuleDetails(ruleDetails.getLatenessRule());
            }

            return ruleDetails;
        } catch (IllegalArgumentException e) {
            throw new InvalidPolicyRuleException("정책 세부 규칙(ruleDetails)의 형식이 잘못되었습니다: " + e.getMessage());
        }
    }

    private void validateStructureByDeductibility(PolicyRuleDetails ruleDetails, boolean isBalanceDeductible) {
        if (isBalanceDeductible) { // true이면 휴가 관련 정책
            if (ruleDetails.getLeaveRule() == null) {
                throw new InvalidPolicyRuleException("잔고 차감이 있는 정책(휴가 등)에는 휴가 규칙(leaveRule)이 필수입니다.");
            }
        } else { // false이면 근무시간 관련 정책
            if (ruleDetails.getAuthRule() == null || ruleDetails.getWorkTimeRule() == null || ruleDetails.getBreakRule() == null) {
                throw new InvalidPolicyRuleException("잔고 차감이 없는 정책(근무 등)에는 인증, 근무 시간, 휴게 규칙이 필수입니다.");
            }
        }
    }

    private void validateAuthRuleDetails(AuthRuleDto authRule) {
        if (authRule.getMethods() != null) {
            for (AuthMethodDto method : authRule.getMethods()) {
                if (method.getDeviceType() == null || method.getAuthMethod() == null) {
                    throw new InvalidPolicyRuleException("인증 규칙에 deviceType 또는 authMethod가 누락되었습니다.");
                }
                Map<String, Object> details = method.getDetails();
                if (details == null || details.isEmpty()) {
                    throw new InvalidPolicyRuleException(method.getDeviceType() + "의 인증 세부 규칙(details)이 없습니다.");
                }
                switch (method.getAuthMethod()) {
                    case "GPS":
                        if (!details.containsKey("gpsRadiusMeters") || !details.containsKey("officeLatitude") || !details.containsKey("officeLongitude")) {
                            throw new InvalidPolicyRuleException("GPS 인증 방식에는 gpsRadiusMeters, officeLatitude, officeLongitude가 필수입니다.");
                        }
                        break;
                    case "NETWORK_IP":
                        if (!details.containsKey("allowedIps")) {
                            throw new InvalidPolicyRuleException("IP 인증 방식에는 allowedIps가 필수입니다.");
                        }
                        break;
                    default:
                        throw new InvalidPolicyRuleException("지원하지 않는 인증 방식입니다: " + method.getAuthMethod());
                }
            }
        }
    }

    private void validateWorkTimeRuleDetails(PolicyRuleDetails ruleDetails) {
        WorkTimeRuleDto workTimeRule = ruleDetails.getWorkTimeRule();
        if (workTimeRule.getType() == null) {
            throw new InvalidPolicyRuleException("근무 시간 규칙에는 type이 필수입니다.");
        }
        switch (workTimeRule.getType()) {
            case "FIXED":
                if (workTimeRule.getFixedWorkMinutes() == null) {
                    throw new InvalidPolicyRuleException("고정 근무제에는 fixedWorkMinutes가 필수입니다.");
                }

                // 법정 필수: 일일 최대 12시간 (근로기준법 제53조)
                if (workTimeRule.getFixedWorkMinutes() > 720) {
                    throw new InvalidPolicyRuleException("일일 근무시간은 최대 12시간(720분)을 초과할 수 없습니다. (근로기준법 제53조)");
                }

                // 법정 필수: 8시간 이상 근무 시 60분 이상 휴게 (근로기준법 제54조)
                if (workTimeRule.getFixedWorkMinutes() >= 480) {
                    BreakRuleDto breakRule = ruleDetails.getBreakRule();
                    if (breakRule == null || breakRule.getMandatoryBreakMinutes() == null || breakRule.getMandatoryBreakMinutes() < 60) {
                        throw new InvalidPolicyRuleException("8시간 이상 근무 시, 최소 60분 이상의 휴게 규칙(breakRule)이 필수입니다. (근로기준법 제54조)");
                    }
                } else if (workTimeRule.getFixedWorkMinutes() >= 240) {
                    // 법정 필수: 4시간 이상 근무 시 30분 이상 휴게
                    BreakRuleDto breakRule = ruleDetails.getBreakRule();
                    if (breakRule == null || breakRule.getMandatoryBreakMinutes() == null || breakRule.getMandatoryBreakMinutes() < 30) {
                        throw new InvalidPolicyRuleException("4시간 이상 근무 시, 최소 30분 이상의 휴게 규칙(breakRule)이 필수입니다. (근로기준법 제54조)");
                    }
                }
                break;
            case "FLEXIBLE":
                if (workTimeRule.getCoreTimeStart() == null || workTimeRule.getCoreTimeEnd() == null) {
                    throw new InvalidPolicyRuleException("선택적 근무제에는 coreTimeStart와 coreTimeEnd가 필수입니다.");
                }
                break;
        }
    }

    private void validateLeaveRuleDetails(LeaveRuleDto leaveRule) {
        if (leaveRule.getAccrualType() == null || leaveRule.getDefaultDays() == null) {
            throw new InvalidPolicyRuleException("휴가 규칙에는 발생 유형(accrualType)과 기본 부여 일수(defaultDays)가 필수입니다.");
        }

        // 법정 필수: 1년 이상 근무자 최소 15일 연차 (근로기준법 제60조)
        if (leaveRule.getDefaultDays() < 15) {
            throw new InvalidPolicyRuleException("1년 이상 근무자의 연차는 최소 15일 이상이어야 합니다. (근로기준법 제60조)");
        }

        // 법정 필수: 1년 미만 근무자 월차 최대 11일 (근로기준법 제60조)
        if (leaveRule.getFirstYearMaxAccrual() != null && leaveRule.getFirstYearMaxAccrual() > 11) {
            throw new InvalidPolicyRuleException("1년 미만 근무자의 연차는 최대 11일입니다. (근로기준법 제60조)");
        }
    }

    private void validateTripRuleDetails(TripRuleDto tripRule) {
        // 출장 규칙은 선택사항 (회사 재량)
        // 설정된 경우에만 유효성 검증

        if (tripRule.getPerDiemAmount() != null && tripRule.getPerDiemAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new InvalidPolicyRuleException("일비는 음수일 수 없습니다.");
        }

        if (tripRule.getAccommodationLimit() != null && tripRule.getAccommodationLimit().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new InvalidPolicyRuleException("숙박비 한도는 음수일 수 없습니다.");
        }

        if (tripRule.getTransportationLimit() != null && tripRule.getTransportationLimit().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new InvalidPolicyRuleException("교통비 한도는 음수일 수 없습니다.");
        }
    }

    private void validateGoOutRuleDetails(GoOutRuleDto goOutRule) {
        // 외출 규칙은 선택사항 (회사 재량)
        // 설정된 경우에만 유효성 검증

        if (goOutRule.getMaxSingleGoOutMinutes() != null && goOutRule.getMaxSingleGoOutMinutes() < 0) {
            throw new InvalidPolicyRuleException("1회 최대 외출 시간은 음수일 수 없습니다.");
        }

        if (goOutRule.getMaxDailyGoOutMinutes() != null && goOutRule.getMaxDailyGoOutMinutes() < 0) {
            throw new InvalidPolicyRuleException("일일 최대 외출 시간은 음수일 수 없습니다.");
        }
    }

    private void validateBreakRuleDetails(BreakRuleDto breakRule) {
        if (breakRule.getType() == null) {
            throw new InvalidPolicyRuleException("휴게 규칙에는 유형(type)이 필수입니다.");
        }

        // 법정 휴게시간이 설정되어 있다면 합리적인 범위인지 검증
        if (breakRule.getMandatoryBreakMinutes() != null) {
            if (breakRule.getMandatoryBreakMinutes() < 0) {
                throw new InvalidPolicyRuleException("법정 휴게시간은 음수일 수 없습니다.");
            }
            // 법정 최소 휴게시간은 4시간 근무 시 30분 (근로기준법 제54조)
            // 구체적인 검증은 WorkTimeRuleDto와 함께 수행됨
        }

        // 일일 최대 휴게시간이 설정되어 있다면 음수 체크
        if (breakRule.getMaxDailyBreakMinutes() != null && breakRule.getMaxDailyBreakMinutes() < 0) {
            throw new InvalidPolicyRuleException("일일 최대 휴게시간은 음수일 수 없습니다.");
        }
    }

    private void validateLatenessRuleDetails(LatenessRuleDto latenessRule) {
        // 지각/조퇴 규칙은 선택사항 (회사 재량)
        // 설정된 경우에만 유효성 검증

        if (latenessRule.getLatenessGraceMinutes() != null && latenessRule.getLatenessGraceMinutes() < 0) {
            throw new InvalidPolicyRuleException("지각 허용 시간은 음수일 수 없습니다.");
        }

        if (latenessRule.getEarlyLeaveGraceMinutes() != null && latenessRule.getEarlyLeaveGraceMinutes() < 0) {
            throw new InvalidPolicyRuleException("조퇴 허용 시간은 음수일 수 없습니다.");
        }
    }
}
