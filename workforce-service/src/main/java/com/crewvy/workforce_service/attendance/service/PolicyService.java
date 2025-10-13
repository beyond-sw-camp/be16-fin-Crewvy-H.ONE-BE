package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.dto.request.PolicyCreateRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyResponse;
import com.crewvy.workforce_service.attendance.dto.rule.AuthMethodDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.crewvy.workforce_service.attendance.dto.rule.WorkTimeRuleDto;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyType;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

            // 2. 각 규칙 블록별 '내부' 유효성 검증
            validateAuthRuleDetails(ruleDetails);
            validateWorkTimeRuleDetails(ruleDetails);
            validateLeaveRuleDetails(ruleDetails);
            validateTripRuleDetails(ruleDetails);
            validateGoOutRuleDetails(ruleDetails);
            validateBreakRuleDetails(ruleDetails);
            validateLatenessRuleDetails(ruleDetails);

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

    private void validateAuthRuleDetails(PolicyRuleDetails ruleDetails) {
        if (ruleDetails.getAuthRule() != null && ruleDetails.getAuthRule().getMethods() != null) {
            for (AuthMethodDto method : ruleDetails.getAuthRule().getMethods()) {
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

    private void validateWorkTimeRuleDetails(PolicyRuleDetails ruleDetails) {
        if (ruleDetails.getWorkTimeRule() != null) {
            WorkTimeRuleDto workTimeRule = ruleDetails.getWorkTimeRule();
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
    }

    private void validateLeaveRuleDetails(PolicyRuleDetails ruleDetails) {
        if (ruleDetails.getLeaveRule() != null) {
            if (ruleDetails.getLeaveRule().getType() == null) {
                throw new BusinessException("휴가 규칙에는 type이 필수입니다.");
            }
        }
    }

    private void validateTripRuleDetails(PolicyRuleDetails ruleDetails) {
        if (ruleDetails.getTripRule() != null) {
            if (ruleDetails.getTripRule().getType() == null) {
                throw new BusinessException("출장 규칙에는 type이 필수입니다.");
            }
        }
    }

    private void validateGoOutRuleDetails(PolicyRuleDetails ruleDetails) {
        if (ruleDetails.getGoOutRuleDto() != null) {
            if (ruleDetails.getGoOutRuleDto().getType() == null) {
                throw new BusinessException("외출 규칙에는 type이 필수입니다.");
            }
        }
    }

    private void validateBreakRuleDetails(PolicyRuleDetails ruleDetails) {
        if (ruleDetails.getBreakRule() != null) {
            if (ruleDetails.getBreakRule().getType() == null) {
                throw new BusinessException("휴게 규칙에는 type이 필수입니다.");
            }
        }
    }

    private void validateLatenessRuleDetails(PolicyRuleDetails ruleDetails) {
        if (ruleDetails.getLatenessRule() != null) {
            if (ruleDetails.getLatenessRule().getDeductionType() == null) {
                throw new BusinessException("지각/조퇴 규칙에는 deductionType이 필수입니다.");
            }
        }
    }


}
