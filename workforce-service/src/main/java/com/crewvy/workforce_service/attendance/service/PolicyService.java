package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.dto.request.PolicyCreateRequest;
import com.crewvy.workforce_service.attendance.dto.request.PolicyUpdateRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyResponse;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyType;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final ObjectMapper objectMapper; // JSON 변환을 위해 주입

    public PolicyResponse createPolicy(UUID companyId, PolicyCreateRequest request) {
        // 1. companyId와 typeCode로 PolicyType이 존재하는지 확인
        PolicyType policyType = policyTypeRepository.findByCompanyIdAndTypeCode(companyId, request.getTypeCode())
                .orElseThrow(() -> new BusinessException("해당 회사에 존재하지 않는 정책 유형입니다."));

        // 2. ruleDetails(Map)를 PolicyRuleDetails DTO 객체로 변환 및 검증
        PolicyRuleDetails ruleDetails = convertAndValidateRuleDetails(request.getRuleDetails(), request.getTypeCode());

        // 3. Policy 엔티티 생성
        Policy newPolicy = Policy.builder()
                .companyId(companyId)
                .policyType(policyType)
                .name(request.getName())
                .isPaid(request.getIsPaid())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .ruleDetails(ruleDetails)
                .build();

        // 4. DB에 저장
        Policy savedPolicy = policyRepository.save(newPolicy);

        // 5. 응답 DTO로 변환하여 반환
        return new PolicyResponse(savedPolicy);
    }

    @Transactional(readOnly = true)
    public PolicyResponse findPolicyById(UUID policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException("ID에 해당하는 정책을 찾을 수 없습니다: " + policyId));
        return new PolicyResponse(policy);
    }

    @Transactional(readOnly = true)
    public Page<PolicyResponse> findAllPoliciesByCompany(UUID companyId, Pageable pageable) {
        Page<Policy> policyPage = policyRepository.findByCompanyId(companyId, pageable);
        return policyPage.map(PolicyResponse::new);
    }

    public PolicyResponse updatePolicy(UUID policyId, PolicyUpdateRequest request) {
        // 1. PolicyType이 존재하는지 확인
        PolicyType policyType = policyTypeRepository.findById(request.getPolicyTypeId())
                .orElseThrow(() -> new BusinessException("존재하지 않는 정책 유형입니다."));

        // 2. 수정할 Policy 엔티티를 조회
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException("ID에 해당하는 정책을 찾을 수 없습니다: " + policyId));

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

        // 5. 변경된 엔티티를 저장 (JPA의 Dirty Checking 덕분에 save 호출은 선택 사항)
        // policyRepository.save(policy);

        // 6. 응답 DTO로 변환하여 반환
        return new PolicyResponse(policy);
    }

    /**
     * Map으로 받은 ruleDetails를 PolicyRuleDetails DTO로 변환하고 유효성을 검증합니다.
     */
    private PolicyRuleDetails convertAndValidateRuleDetails(Map<String, Object> ruleDetailsMap, PolicyTypeCode typeCode) {
        if (ruleDetailsMap == null || ruleDetailsMap.isEmpty()) {
            if (typeCode == PolicyTypeCode.STANDARD_WORK) {
                throw new BusinessException("기본 근무 정책에는 세부 규칙(ruleDetails)이 필수입니다.");
            }
            return null;
        }
        try {
            PolicyRuleDetails ruleDetails = objectMapper.convertValue(ruleDetailsMap, PolicyRuleDetails.class);

            // 1. PolicyTypeCode에 따른 '구조적' 유효성 검증
            validateStructureByTypeCode(ruleDetails, typeCode);

            // 2. 존재하는 규칙 블록에 대해서만 '내부' 유효성 검증을 선택적으로 실행
            if (ruleDetails.getAuthRule() != null) {
                validateAuthRuleDetails(ruleDetails.getAuthRule());
            }
            if (ruleDetails.getWorkTimeRule() != null) {
                validateWorkTimeRuleDetails(ruleDetails.getWorkTimeRule());
            }
            if (ruleDetails.getLeaveRule() != null) {
                validateLeaveRuleDetails(ruleDetails.getLeaveRule());
            }
            if (ruleDetails.getTripRule() != null) {
                validateTripRuleDetails(ruleDetails.getTripRule());
            }
            if (ruleDetails.getGoOutRuleDto() != null) {
                validateGoOutRuleDetails(ruleDetails.getGoOutRuleDto());
            }
            if (ruleDetails.getBreakRule() != null) {
                validateBreakRuleDetails(ruleDetails.getBreakRule());
            }
            if (ruleDetails.getLatenessRule() != null) {
                validateLatenessRuleDetails(ruleDetails.getLatenessRule());
            }

            return ruleDetails;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("정책 세부 규칙(ruleDetails)의 형식이 잘못되었습니다: " + e.getMessage(), e);
        }
    }

    private void validateStructureByTypeCode(PolicyRuleDetails ruleDetails, PolicyTypeCode typeCode) {
        switch (typeCode) {
            case STANDARD_WORK:
                if (ruleDetails.getAuthRule() == null || ruleDetails.getWorkTimeRule() == null) {
                    throw new BusinessException("기본 근무 정책에는 인증 규칙(authRule)과 근무 시간 규칙(workTimeRule)이 필수입니다.");
                }
                break;
            case ANNUAL_LEAVE:
                if (ruleDetails.getLeaveRule() == null) {
                    throw new BusinessException("연차 정책에는 휴가 규칙(leaveRule)이 필수입니다.");
                }
                break;
            // TODO: 다른 PolicyTypeCode에 대한 검증 케이스 추가
        }
    }

    private void validateAuthRuleDetails(AuthRuleDto authRule) {
        if (authRule.getMethods() != null) {
            for (AuthMethodDto method : authRule.getMethods()) {
                if (method.getDeviceType() == null || method.getAuthMethod() == null) {
                    throw new BusinessException("인증 규칙에 deviceType 또는 authMethod가 누락되었습니다.");
                }
                Map<String, Object> details = method.getDetails();
                if (details == null || details.isEmpty()) {
                    throw new BusinessException(method.getDeviceType() + "의 인증 세부 규칙(details)이 없습니다.");
                }
                switch (method.getAuthMethod()) {
                    case "GPS":
                        if (!details.containsKey("gpsRadiusMeters") || !details.containsKey("officeLatitude") || !details.containsKey("officeLongitude")) {
                            throw new BusinessException("GPS 인증 방식에는 gpsRadiusMeters, officeLatitude, officeLongitude가 필수입니다.");
                        }
                        break;
                    case "NETWORK_IP":
                        if (!details.containsKey("allowedIps")) {
                            throw new BusinessException("IP 인증 방식에는 allowedIps가 필수입니다.");
                        }
                        break;
                    default:
                        throw new BusinessException("지원하지 않는 인증 방식입니다: " + method.getAuthMethod());
                }
            }
        }
    }

    private void validateWorkTimeRuleDetails(WorkTimeRuleDto workTimeRule) {
        if (workTimeRule.getType() == null) {
            throw new BusinessException("근무 시간 규칙에는 type이 필수입니다.");
        }
        switch (workTimeRule.getType()) {
            case "FIXED":
                if (workTimeRule.getFixedWorkMinutes() == null) {
                    throw new BusinessException("고정 근무제에는 fixedWorkMinutes가 필수입니다.");
                }
                break;
            case "FLEXIBLE":
                if (workTimeRule.getCoreTimeStart() == null || workTimeRule.getCoreTimeEnd() == null) {
                    throw new BusinessException("선택적 근무제에는 coreTimeStart와 coreTimeEnd가 필수입니다.");
                }
                break;
        }
    }

    private void validateLeaveRuleDetails(LeaveRuleDto leaveRule) {
        if (leaveRule.getType() == null) {
            throw new BusinessException("휴가 규칙에는 type이 필수입니다.");
        }
    }

    private void validateTripRuleDetails(TripRuleDto tripRule) {
        if (tripRule.getType() == null) {
            throw new BusinessException("출장 규칙에는 type이 필수입니다.");
        }
    }

    private void validateGoOutRuleDetails(GoOutRuleDto goOutRule) {
        if (goOutRule.getType() == null) {
            throw new BusinessException("외출 규칙에는 type이 필수입니다.");
        }
    }

    private void validateBreakRuleDetails(BreakRuleDto breakRule) {
        if (breakRule.getType() == null) {
            throw new BusinessException("휴게 규칙에는 type이 필수입니다.");
        }
    }

    private void validateLatenessRuleDetails(LatenessRuleDto latenessRule) {
        if (latenessRule.getDeductionType() == null) {
            throw new BusinessException("지각/조퇴 규칙에는 deductionType이 필수입니다.");
        }
    }
}
