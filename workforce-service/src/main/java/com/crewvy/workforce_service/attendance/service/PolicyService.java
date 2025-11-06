package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.dto.request.PolicyCreateRequest;
import com.crewvy.workforce_service.attendance.dto.request.PolicyUpdateRequest;
import com.crewvy.workforce_service.attendance.dto.response.ApplicablePolicyResponse;
import com.crewvy.workforce_service.attendance.dto.response.PolicyResponse;
import com.crewvy.workforce_service.attendance.dto.response.PolicyTypeResponse;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyType;
import com.crewvy.workforce_service.attendance.repository.PolicyAssignmentRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyTypeRepository;
import com.crewvy.workforce_service.attendance.validation.PolicyRuleValidator;
import com.crewvy.workforce_service.attendance.validation.PolicyValidatorFactory;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final PolicyAssignmentService policyAssignmentService;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final ObjectMapper objectMapper;
    private final MemberClient memberClient;
    private final PolicyValidatorFactory validatorFactory;

    public PolicyResponse createPolicy(UUID memberpositionId, UUID companyId, UUID organizationId, PolicyCreateRequest request) {
//        checkPermissionOrThrow(memberpositionId, "CREATE", "COMPANY", "회사 정책 생성 권한이 없습니다.");

//        PolicyType policyType = policyTypeRepository.findByCompanyIdAndTypeCode(companyId, request.getTypeCode())
        PolicyType policyType =  policyTypeRepository.findByTypeCode(request.getTypeCode())
                .orElseThrow(() -> new ResourceNotFoundException("해당 회사에 존재하지 않는 정책 유형입니다."));

        // 법정 필수 유급 휴가 검증
        validateMandatoryPaidLeave(request.getTypeCode(), request.getIsPaid());

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
                .autoApprove(request.getAutoApprove())
                .build();

        Policy savedPolicy = policyRepository.save(newPolicy);
        return new PolicyResponse(savedPolicy);
    }

    @Transactional(readOnly = true)
    public PolicyResponse findPolicyById(UUID memberpositionId, UUID companyId, UUID policyId) {
//        checkPermissionOrThrow(memberpositionId, "READ", "COMPANY", "회사 정책 조회 권한이 없습니다.");

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("ID에 해당하는 정책을 찾을 수 없습니다: " + policyId));
        return new PolicyResponse(policy);
    }

    @Transactional(readOnly = true)
    public Page<PolicyResponse> findAllPoliciesByCompany(UUID memberpositionId, UUID companyId, Pageable pageable) {
//        checkPermissionOrThrow(memberpositionId, "READ", "COMPANY", "회사 정책 조회 권한이 없습니다.");

        Page<Policy> policyPage = policyRepository.findByCompanyId(companyId, pageable);
        return policyPage.map(PolicyResponse::new);
    }

    public PolicyResponse updatePolicy(UUID memberpositionId, UUID companyId, UUID policyId, PolicyUpdateRequest request) {
//        checkPermissionOrThrow(memberpositionId, "UPDATE", "COMPANY", "회사 정책 수정 권한이 없습니다.");
        // 1. 수정할 Policy 엔티티를 조회
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("ID에 해당하는 정책을 찾을 수 없습니다: " + policyId));

        // 2. PolicyType이 존재하는지 확인
//        PolicyType policyType = policyTypeRepository.findByCompanyIdAndTypeCode(policy.getCompanyId(), request.getTypeCode())
        PolicyType policyType = policyTypeRepository.findByTypeCode(request.getTypeCode())
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 정책 유형입니다."));

        // 3. 법정 필수 유급 휴가 검증
        validateMandatoryPaidLeave(request.getTypeCode(), request.getIsPaid());

        // 4. ruleDetails 변환 및 검증
        PolicyRuleDetails ruleDetails = convertAndValidateRuleDetails(request.getRuleDetails(), policyType.getTypeCode());

        // 5. 엔티티 내용 업데이트
        policy.update(
                policyType,
                request.getName(),
                request.getIsPaid(),
                request.getEffectiveFrom(),
                request.getEffectiveTo(),
                ruleDetails,
                request.getAutoApprove()
        );
        return new PolicyResponse(policy);
    }

    public void deletePolicy(UUID memberpositionId, UUID companyId, UUID policyId) {
//        checkPermissionOrThrow(memberpositionId, "DELETE", "COMPANY", "회사 정책 삭제 권한이 없습니다.");

        // 삭제하려는 정책이 '활성 상태'로 다른 곳에 할당되어 있는지 확인
        if (policyAssignmentRepository.existsByPolicy_IdAndIsActiveTrue(policyId)) {
            throw new BusinessException("이 정책은 현재 활성 상태로 할당되어 있어 삭제할 수 없습니다. 할당을 먼저 해지해주세요.");
        }

        if (!policyRepository.existsById(policyId)) {
            throw new ResourceNotFoundException("ID에 해당하는 정책을 찾을 수 없습니다: " + policyId);
        }
        policyRepository.deleteById(policyId);
    }

    public void activatePolicies(UUID memberpositionId, UUID companyId, List<UUID> policyIds) {
//        checkPermissionOrThrow(memberpositionId, "UPDATE", "COMPANY", "회사 정책 수정 권한이 없습니다.");

        List<Policy> policiesToActivate = policyRepository.findAllById(policyIds);
        if (policiesToActivate.size() != policyIds.size()) {
            throw new BusinessException("요청된 ID 목록에 존재하지 않는 정책이 포함되어 있습니다.");
        }
        // 요청된 모든 정책을 활성화 상태로 변경
        policiesToActivate.forEach(Policy::activate);
    }

    public void deactivatePolicies(UUID memberpositionId, UUID companyId, List<UUID> policyIds) {
        checkPermission(memberpositionId, "attendance", "UPDATE", "COMPANY");

        List<Policy> policies = policyRepository.findAllById(policyIds);
        policies.forEach(Policy::deactivate);
        policyRepository.saveAll(policies);
    }

    public List<PolicyTypeResponse> findPolicyTypesByCompany(UUID memberpositionId, UUID companyId) {
        checkPermission(memberpositionId, "attendance", "READ", "COMPANY");

//        List<PolicyType> policyTypes = policyTypeRepository.findByCompanyId(companyId);
        List<PolicyType> policyTypes = policyTypeRepository.findAll();
        return policyTypes.stream().map(PolicyTypeResponse::new).collect(Collectors.toList());
    }

    public PolicyResponse findMyEffectivePolicy(UUID memberId, UUID memberPositionId, UUID companyId) {
        // 기본근무(STANDARD_WORK) 정책을 조회 (휴게 규칙, 근무 시간 규칙 등 포함)
        Policy effectivePolicy = policyAssignmentService.findEffectivePolicyForMemberByType(
                memberId,
                memberPositionId,
                companyId,
                PolicyTypeCode.STANDARD_WORK
        );

        if (effectivePolicy == null) {
            throw new ResourceNotFoundException("할당된 기본근무 정책을 찾을 수 없습니다.");
        }

        return new PolicyResponse(effectivePolicy);
    }

    @Transactional(readOnly = true)
    public List<ApplicablePolicyResponse> findApplicablePoliciesForMember(UUID memberId, UUID memberPositionId, UUID companyId, UUID organizationId) {
        // 사용자가 '신청'할 수 있는 정책 유형들을 정의합니다. (기본근무 등은 신청 대상이 아님)
        List<PolicyTypeCode> requestablePolicyTypes = Arrays.asList(
                PolicyTypeCode.ANNUAL_LEAVE,
                PolicyTypeCode.BUSINESS_TRIP,
                PolicyTypeCode.MATERNITY_LEAVE,
                PolicyTypeCode.PATERNITY_LEAVE,
                PolicyTypeCode.CHILDCARE_LEAVE,
                PolicyTypeCode.FAMILY_CARE_LEAVE,
                PolicyTypeCode.MENSTRUAL_LEAVE
                // 필요에 따라 연장/야간/휴일 근무 신청 정책도 추가 가능
        );

        // 각 정책 유형에 대해 현재 사용자에게 유효한 정책이 있는지 확인하고, 있으면 리스트에 추가합니다.
        return requestablePolicyTypes.stream()
                .map(typeCode -> policyAssignmentService.findEffectivePolicyForMemberByType(memberId, memberPositionId, companyId, typeCode))
                .filter(Objects::nonNull) // 유효한 정책이 없는 경우(null)는 제외합니다.
                .map(ApplicablePolicyResponse::from)
                .collect(Collectors.toList());
    }

    private PolicyRuleDetails convertAndValidateRuleDetails(Map<String, Object> rawDetails, PolicyTypeCode typeCode) {
        if (rawDetails == null) {
            return new PolicyRuleDetails();
        }
        try {
            PolicyRuleDetails ruleDetails = objectMapper.convertValue(rawDetails, PolicyRuleDetails.class);

            PolicyRuleValidator validator = validatorFactory.getValidator(typeCode);
            validator.validate(ruleDetails);

            return ruleDetails;
        } catch (IllegalArgumentException e) {
            throw new InvalidPolicyRuleException("제공된 규칙 상세 정보(ruleDetails)의 형식이 올바르지 않습니다.");
        }
    }

    /**
     * 권한 체크 헬퍼 메서드
     * @param memberPositionId 권한을 확인할 직원의 memberPositionId
     * @param resource 리소스 (예: "attendance")
     * @param action 액션 (예: "CREATE", "READ", "UPDATE", "DELETE")
     * @param range 권한 범위 (예: "INDIVIDUAL", "DEPARTMENT", "COMPANY")
     * @throws com.crewvy.common.exception.PermissionDeniedException 권한이 없는 경우
     */
    private void checkPermission(UUID memberPositionId, String resource, String action, String range) {
        Boolean hasPermission = memberClient.checkPermission(memberPositionId, resource, action, range).getData();
        if (hasPermission == null || !hasPermission) {
            throw new com.crewvy.common.exception.PermissionDeniedException("권한이 없습니다.");
        }
    }

    /**
     * 법정 필수 유급 휴가의 유급 여부를 검증합니다.
     * @param typeCode 정책 타입 코드
     * @param isPaid 유급 여부
     * @throws BusinessException 법정 필수 유급 휴가가 무급으로 설정된 경우
     */
    private void validateMandatoryPaidLeave(PolicyTypeCode typeCode, Boolean isPaid) {
        if (isPaid == null || !isPaid) {
            switch (typeCode) {
                case ANNUAL_LEAVE:
                    throw new BusinessException("연차유급휴가는 반드시 유급이어야 합니다. (근로기준법 제60조)");
                case MATERNITY_LEAVE:
                    throw new BusinessException("출산전후휴가는 반드시 유급이어야 합니다. (근로기준법 제74조)");
                case PATERNITY_LEAVE:
                    throw new BusinessException("배우자 출산휴가는 반드시 유급이어야 합니다. (남녀고용평등법 제18조의2)");
            }
        }
    }
}




