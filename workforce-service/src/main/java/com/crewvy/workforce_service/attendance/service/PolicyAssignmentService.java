package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import com.crewvy.workforce_service.attendance.dto.request.PolicyAssignmentRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyAssignmentResponse;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import com.crewvy.workforce_service.attendance.repository.PolicyAssignmentRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PolicyAssignmentService {

    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final PolicyRepository policyRepository;
    private final MemberClient memberClient;

    public void assignPoliciesToTargets(UUID memberPositionId, PolicyAssignmentRequest request) {
        checkPermissionOrThrow(memberPositionId, "UPDATE", "COMPANY", "정책을 할당할 권한이 없습니다.");

        for (PolicyAssignmentRequest.SingleAssignmentRequest assignmentReq : request.getAssignments()) {
            Policy policy = policyRepository.findById(assignmentReq.getPolicyId())
                    .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 정책입니다: " + assignmentReq.getPolicyId()));

            // TODO: targetId 유효성 검증 (member-service 호출)

            if (policyAssignmentRepository.existsByPolicyAndTargetIdAndTargetType(
                    policy, assignmentReq.getTargetId(), assignmentReq.getScopeType())) {
                continue; // 이미 할당된 경우 건너뛰기
            }

            PolicyAssignment newAssignment = PolicyAssignment.builder()
                    .policy(policy)
                    .targetId(assignmentReq.getTargetId())
                    .targetType(assignmentReq.getScopeType())
                    .isActive(true)
                    .assignedBy(memberPositionId)
                    .assignedAt(LocalDateTime.now())
                    .build();
            policyAssignmentRepository.save(newAssignment);
        }
    }

    @Transactional(readOnly = true)
    public Policy findEffectivePolicyForMember(UUID memberId, UUID companyId) {
        // 1. MEMBER 우선: 직원에게 직접 할당된 정책 조회
        Optional<Policy> memberPolicy = policyAssignmentRepository
                .findFirstByTargetIdAndTargetTypeAndIsActiveTrueOrderByAssignedAtDesc(memberId, PolicyScopeType.MEMBER)
                .map(PolicyAssignment::getPolicy);
        if (memberPolicy.isPresent()) {
            return memberPolicy.get();
        }

        // 2. ORGANIZATION 우선: 직원의 소속 조직(상위 포함)에 할당된 정책 조회
        // 조직 계층 구조 순서대로 조회 (하위 조직부터 상위 조직 순으로)
        ApiResponse<List<UUID>> hierarchyResponse = memberClient.getOrganizationHierarchy(memberId);
        if (hierarchyResponse != null && hierarchyResponse.getData() != null && !hierarchyResponse.getData().isEmpty()) {
            List<UUID> organizationHierarchy = hierarchyResponse.getData();
            // 계층 구조 순서대로 순회하며 첫 번째 할당된 정책 반환
            for (UUID organizationId : organizationHierarchy) {
                Optional<PolicyAssignment> assignment = policyAssignmentRepository
                        .findFirstByTargetIdAndTargetTypeAndIsActiveTrueOrderByAssignedAtDesc(organizationId, PolicyScopeType.ORGANIZATION);
                if (assignment.isPresent()) {
                    return assignment.get().getPolicy();
                }
            }
        }

        // 3. COMPANY 우선: 회사 전체에 할당된 정책 조회
        Optional<Policy> companyPolicy = policyAssignmentRepository
                .findFirstByTargetIdAndTargetTypeAndIsActiveTrueOrderByAssignedAtDesc(companyId, PolicyScopeType.COMPANY)
                .map(PolicyAssignment::getPolicy);
        if (companyPolicy.isPresent()) {
            return companyPolicy.get();
        }

        // 4. 할당된 정책이 없을 경우, 회사 기본 정책 반환
        return findDefaultCompanyPolicy(companyId);
    }

    private Policy findDefaultCompanyPolicy(UUID companyId) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        return policyRepository.findActivePolicies(companyId, LocalDate.now(), pageable)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("적용할 수 있는 정책이 회사에 존재하지 않습니다."));
    }

    @Transactional(readOnly = true)
    public List<PolicyAssignmentResponse> findPolicyAssignmentsByTarget(UUID memberPositionId, UUID targetId, PolicyScopeType targetType) {
        checkPermissionOrThrow(memberPositionId, "READ", "COMPANY", "정책 할당 내역을 조회할 권한이 없습니다.");

        List<PolicyAssignment> assignments = policyAssignmentRepository.findByTargetIdAndTargetTypeOrderByAssignedAtDesc(targetId, targetType);
        return assignments.stream()
                .map(PolicyAssignmentResponse::new)
                .collect(Collectors.toList());
    }

    private void checkPermissionOrThrow(UUID memberPositionId, String action, String range, String errorMessage) {
        ApiResponse<Boolean> response = memberClient.checkPermission(memberPositionId, "attendance", action, range);
        if (response == null || !Boolean.TRUE.equals(response.getData())) {
            throw new PermissionDeniedException(errorMessage);
        }
    }
}
