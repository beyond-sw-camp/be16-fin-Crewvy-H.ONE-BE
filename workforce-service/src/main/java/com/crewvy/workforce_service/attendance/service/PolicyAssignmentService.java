package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import com.crewvy.workforce_service.attendance.dto.request.PolicyAssignmentRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyAssignmentResponse;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import com.crewvy.workforce_service.attendance.repository.PolicyAssignmentRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.MemberPositionListRes;
import com.crewvy.workforce_service.feignClient.dto.response.OrganizationNodeDto;
import com.crewvy.workforce_service.feignClient.dto.response.OrganizationRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyAssignmentService {

    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final PolicyRepository policyRepository;
    private final MemberClient memberClient;

    /**
     * 직원에 대한 유효 정책을 계층 구조 우선순위에 따라 조회합니다.
     * 우선순위: 개인 > 직속 조직 > 상위 조직 ... > 회사
     * @param memberId 직원 ID
     * @param companyId 회사 ID
     * @param memberPositionId 직원의 직책 ID
     * @return 적용될 최종 정책
     */
    public Policy findEffectivePolicyForMember(UUID memberId, UUID companyId, UUID memberPositionId) { // organizationId -> memberPositionId
        List<OrganizationRes> orgPath = new ArrayList<>();
        try {
            ApiResponse<List<OrganizationRes>> response = memberClient.getOrganizationList(memberPositionId); // 파라미터 변경
            if (response != null && response.getData() != null) {
                orgPath = response.getData();
            }
        } catch (Exception e) {
            throw new BusinessException("Member-service에서 조직 정보를 가져오는 데 실패했습니다.", e);
        }

        List<UUID> priorityList = new ArrayList<>();
        priorityList.add(memberId);
        orgPath.forEach(org -> priorityList.add(org.getId()));

        // 1. 우선순위 경로에 있는 모든 활성 정책을 한 번에 조회
        List<PolicyAssignment> assignments = policyAssignmentRepository.findActiveAssignmentsByTargets(priorityList, LocalDateTime.now().toLocalDate());

        // 2. 우선순위(priority)를 기준으로 정책들을 그룹핑
        Map<Integer, List<PolicyAssignment>> assignmentsByPriority = assignments.stream()
                .collect(Collectors.groupingBy(pa -> pa.getPolicy().getPolicyType().getPriority()));

        // 3. 우선순위가 가장 높은 순서(숫자가 낮은 순)부터 순회
        for (int priority = 1; priority <= 3; priority++) { // 우선순위 레벨은 1, 2, 3으로 가정
            List<PolicyAssignment> currentPriorityAssignments = assignmentsByPriority.get(priority);
            if (currentPriorityAssignments != null) {
                // 4. 현재 우선순위 그룹 내에서, [개인->조직->회사] 순서로 정책 탐색
                for (UUID targetId : priorityList) {
                    for (PolicyAssignment assignment : currentPriorityAssignments) {
                        if (assignment.getTargetId().equals(targetId)) {
                            return assignment.getPolicy(); // 가장 먼저 찾은 정책을 반환하고 즉시 종료
                        }
                    }
                }
            }
        }

        throw new ResourceNotFoundException("해당 직원에게 적용되는 유효한 근태 정책이 없습니다. 관리자에게 문의하세요.");
    }

    /**
     * [관리자용] 새로운 정책 할당을 생성합니다.
     */
    @Transactional
    public List<PolicyAssignmentResponse> createAssignments(UUID memberPositionId, PolicyAssignmentRequest request) {
        // TODO: memberPositionId를 사용하여 권한 검사 로직 추가

        List<PolicyAssignment> newAssignments = request.getAssignments().stream()
                .map(singleRequest -> {
                    Policy policy = policyRepository.findById(singleRequest.getPolicyId())
                            .orElseThrow(() -> new ResourceNotFoundException("해당 정책을 찾을 수 없습니다. ID: " + singleRequest.getPolicyId()));

                    return PolicyAssignment.builder()
                            .policy(policy)
                            .scopeType(singleRequest.getScopeType())
                            .targetType(singleRequest.getScopeType()) // targetType 필드 추가
                            .targetId(singleRequest.getTargetId())
                            .assignedBy(memberPositionId) // 할당자 정보 추가
                            .assignedAt(LocalDateTime.now())
                            .isActive(true)
                            .build();
                })
                .collect(Collectors.toList());

        List<PolicyAssignment> savedAssignments = policyAssignmentRepository.saveAll(newAssignments);

        return savedAssignments.stream()
                .map(PolicyAssignmentResponse::new) // 생성자 사용으로 수정
                .collect(Collectors.toList());
    }

    /**
     * [관리자용] 특정 회사의 모든 정책 할당 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<PolicyAssignmentResponse> findAssignments(UUID memberId, UUID memberPositionId, UUID companyId) {
        List<PolicyAssignment> assignments = policyAssignmentRepository.findAllByCompanyId(companyId);

        // 1. 직원 정보 조회
        List<UUID> memberIds = assignments.stream()
                .filter(a -> a.getScopeType() == PolicyScopeType.MEMBER)
                .map(PolicyAssignment::getTargetId).distinct().collect(Collectors.toList());

        final Map<UUID, MemberPositionListRes> memberInfoMap = new HashMap<>();
        if (!memberIds.isEmpty()) {
            try {
                IdListReq idListReq = new IdListReq(memberIds);
                ApiResponse<List<MemberPositionListRes>> response = memberClient.getDefaultPositionList(memberPositionId, idListReq);
                if (response != null && response.getData() != null) {
                    response.getData().forEach(dto -> memberInfoMap.put(dto.getMemberId(), dto));
                }
            } catch (Exception e) {
                log.error("Failed to fetch member info from member-service", e);
            }
        }

        // 2. 회사의 전체 조직도 정보 조회 및 평탄화
        final Map<UUID, String> orgNameMap = new HashMap<>();
        final Map<UUID, UUID> childToParentMap = new HashMap<>();
        final OrganizationNodeDto companyInfo;

        try {
            ApiResponse<List<OrganizationNodeDto>> response = memberClient.getOrganization(memberId);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                companyInfo = response.getData().get(0); // 최상위 노드가 회사

                // 재귀 함수 또는 스택/큐를 사용하여 트리 순회
                Queue<OrganizationNodeDto> queue = new LinkedList<>(response.getData());
                while (!queue.isEmpty()) {
                    OrganizationNodeDto node = queue.poll();
                    orgNameMap.put(node.getId(), node.getLabel());
                    if (node.getChildren() != null) {
                        for (OrganizationNodeDto child : node.getChildren()) {
                            childToParentMap.put(child.getId(), node.getId());
                            queue.add(child);
                        }
                    }
                }
            } else {
                companyInfo = null;
            }
        } catch (Exception e) {
            throw new BusinessException("Member-service에서 조직 정보를 가져오는 데 실패했습니다.", e);
        }

        // 3. 데이터 조합 및 최종 응답 생성
        return assignments.stream()
                .map(assignment -> {
                    PolicyAssignmentResponse responseDto = new PolicyAssignmentResponse(assignment);
                    PolicyScopeType scopeType = assignment.getScopeType();
                    UUID targetId = assignment.getTargetId();

                    switch (scopeType) {
                        case MEMBER:
                            if (memberInfoMap.containsKey(targetId)) {
                                MemberPositionListRes memberInfo = memberInfoMap.get(targetId);
                                responseDto.setTargetName(memberInfo.getMemberName());
                                responseDto.setTargetAffiliation(memberInfo.getOrganizationName());
                            }
                            break;
                        case ORGANIZATION:
                            if (orgNameMap.containsKey(targetId)) {
                                responseDto.setTargetName(orgNameMap.get(targetId));
                                UUID parentId = childToParentMap.get(targetId);
                                String parentName = (parentId != null) ? orgNameMap.get(parentId) : (companyInfo != null ? companyInfo.getLabel() : "-");
                                responseDto.setTargetAffiliation(parentName);
                            }
                            break;
                        case COMPANY:
                            if (companyInfo != null && companyInfo.getId().equals(targetId)) {
                                responseDto.setTargetName(companyInfo.getLabel());
                                responseDto.setTargetAffiliation("-");
                            }
                            break;
                    }
                    return responseDto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAssignment(UUID memberPositionId, UUID assignmentId) {
        // TODO: memberPositionId를 사용하여 권한 검사 로직 추가
        
        PolicyAssignment assignment = policyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 정책 할당을 찾을 수 없습니다. ID: " + assignmentId));
        
        policyAssignmentRepository.delete(assignment);
    }

    /**
     * [관리자용] 특정 대상(target)에 대한 정책 할당 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<PolicyAssignmentResponse> findPolicyAssignmentsByTarget(UUID memberPositionId, UUID targetId, PolicyScopeType scopeType) {
        // TODO: 권한 검사
        List<PolicyAssignment> assignments;
        if (scopeType != null) {
            assignments = policyAssignmentRepository.findByTargetIdAndScopeType(targetId, scopeType);
        } else {
            assignments = policyAssignmentRepository.findByTargetId(targetId);
        }

        // (findAssignments와 동일한 로직으로 이름 채우기 - 현재는 주석 처리)
        return assignments.stream()
                .map(PolicyAssignmentResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * [관리자용] 특정 정책 할당을 해지(비활성화)합니다.
     */
    @Transactional
    public void revokePolicyAssignment(UUID memberPositionId, UUID assignmentId) {
        // TODO: 권한 검사
        PolicyAssignment assignment = policyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 정책 할당을 찾을 수 없습니다. ID: " + assignmentId));

        assignment.deactivate();
    }

    /**
     * [관리자용] 여러 정책 할당을 일괄 해지(비활성화)합니다.
     */
    @Transactional
    public void revokeAssignments(UUID memberPositionId, List<UUID> assignmentIds) {
        // TODO: 권한 검사
        List<PolicyAssignment> assignmentsToRevoke = policyAssignmentRepository.findAllById(assignmentIds);
        if (assignmentsToRevoke.size() != assignmentIds.size()) {
            throw new BusinessException("요청된 ID 목록에 존재하지 않는 할당이 포함되어 있습니다.");
        }
        assignmentsToRevoke.forEach(PolicyAssignment::deactivate);
    }
}
