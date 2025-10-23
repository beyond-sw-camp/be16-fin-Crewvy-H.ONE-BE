package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.InvalidPolicyRuleException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.dto.request.PolicyCreateRequest;
import com.crewvy.workforce_service.attendance.dto.request.PolicyUpdateRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyResponse;
import com.crewvy.workforce_service.attendance.dto.response.PolicyTypeResponse;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyType;
import com.crewvy.workforce_service.attendance.repository.PolicyAssignmentRepository;
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
    private final PolicyAssignmentService policyAssignmentService;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final ObjectMapper objectMapper;
    private final MemberClient memberClient;

    public PolicyResponse createPolicy(UUID memberpositionId, UUID companyId, UUID organizationId, PolicyCreateRequest request) {
//        checkPermissionOrThrow(memberpositionId, "CREATE", "COMPANY", "회사 정책 생성 권한이 없습니다.");

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
        // TODO: 권한 검사
        List<Policy> policies = policyRepository.findAllById(policyIds);
        policies.forEach(Policy::deactivate);
        policyRepository.saveAll(policies);
    }

    public List<PolicyTypeResponse> findPolicyTypesByCompany(UUID memberpositionId, UUID companyId) {
        // TODO: 권한 검사
        List<PolicyType> policyTypes = policyTypeRepository.findByCompanyId(companyId);
        return policyTypes.stream().map(PolicyTypeResponse::new).collect(Collectors.toList());
    }

    public PolicyResponse findMyEffectivePolicy(UUID memberId, UUID companyId, UUID organizationId) {
        Policy effectivePolicy = policyAssignmentService.findEffectivePolicyForMember(memberId, companyId, organizationId);
        return new PolicyResponse(effectivePolicy);
    }

    private PolicyRuleDetails convertAndValidateRuleDetails(Map<String, Object> rawDetails, PolicyTypeCode typeCode) {
        if (rawDetails == null) {
            return new PolicyRuleDetails();
        }
        try {
            PolicyRuleDetails ruleDetails = objectMapper.convertValue(rawDetails, PolicyRuleDetails.class);

            // PolicyTypeCode에 따른 ruleDetails 유효성 검증
            switch (typeCode) {
                case STANDARD_WORK:
                    if (ruleDetails.getWorkTimeRule() == null) {
                        throw new InvalidPolicyRuleException("기본 근무 정책에는 근무 시간 규칙(workTimeRule)이 필수입니다.");
                    }
                    break;
                case ANNUAL_LEAVE:
                case MATERNITY_LEAVE:
                case PATERNITY_LEAVE:
                    // 휴가 관련 정책은 특정 규칙이 필수 아닐 수 있음 (타입 자체가 중요)
                    // 필요 시 여기에 유효성 검증 추가
                    break;
                case BUSINESS_TRIP:
                    if (ruleDetails.getTripRule() == null) {
                        throw new InvalidPolicyRuleException("출장 정책에는 출장 규칙(tripRule)이 필수입니다.");
                    }
                    break;
                // TODO: 다른 PolicyTypeCode에 대한 유효성 검증 케이스 추가
                default:
                    break;
            }
            return ruleDetails;
        } catch (IllegalArgumentException e) {
            throw new InvalidPolicyRuleException("제공된 규칙 상세 정보(ruleDetails)의 형식이 올바르지 않습니다.");
        }
    }
}

