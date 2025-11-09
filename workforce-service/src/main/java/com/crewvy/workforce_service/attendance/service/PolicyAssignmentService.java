package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.attendance.constant.PolicyScopeType;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.dto.request.PolicyAssignmentRequest;
import com.crewvy.workforce_service.attendance.dto.response.PolicyAssignmentResponse;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import com.crewvy.workforce_service.attendance.repository.MemberBalanceRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyAssignmentRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.MemberPositionListRes;
import com.crewvy.workforce_service.feignClient.dto.response.OrganizationNodeDto;
import com.crewvy.workforce_service.feignClient.dto.response.OrganizationRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final MemberBalanceRepository memberBalanceRepository;
    private final AnnualLeaveAccrualService annualLeaveAccrualService;

    /**
     * 직원에 대한 특정 타입의 유효 정책을 계층 구조 우선순위에 따라 조회합니다.
     * 우선순위: 직책(MEMBER_POSITION) > 개인(MEMBER) > 조직(ORGANIZATION) > 상위 조직 ... > 회사(COMPANY)
     * @param memberId 직원 ID
     * @param memberPositionId 직원의 직책 ID
     * @param companyId 회사 ID (멀티테넌트 보안)
     * @param typeCode 조회할 정책 타입 코드
     * @return 적용될 최종 정책 (없으면 null 반환)
     */
    public Policy findEffectivePolicyForMemberByType(UUID memberId, UUID memberPositionId, UUID companyId, PolicyTypeCode typeCode) {
        log.info("[정책 조회 시작] memberId={}, memberPositionId={}, companyId={}, typeCode={}",
                memberId, memberPositionId, companyId, typeCode);

        // 1. 조직 계층 조회 (내 부서 → 상위부서 → 회사)
        List<OrganizationRes> orgPath = new ArrayList<>();
        try {
            ApiResponse<List<OrganizationRes>> response = memberClient.getOrganizationList(memberPositionId);
            if (response != null && response.getData() != null) {
                orgPath = response.getData();
            }
            log.info("[조직 계층 조회] 조직 수={}, 조직 ID 목록={}",
                    orgPath.size(), orgPath.stream().map(OrganizationRes::getId).collect(Collectors.toList()));
        } catch (Exception e) {
            throw new BusinessException("Member-service에서 조직 정보를 가져오는 데 실패했습니다.", e);
        }

        // 2. 탐색 우선순위 구성 (MEMBER_POSITION → MEMBER → ORGANIZATION → 상위 ORG → COMPANY)
        List<PolicyTarget> priorityTargets = new ArrayList<>();
        priorityTargets.add(new PolicyTarget(memberPositionId, PolicyScopeType.MEMBER_POSITION));
        priorityTargets.add(new PolicyTarget(memberId, PolicyScopeType.MEMBER));
        orgPath.forEach(org -> priorityTargets.add(new PolicyTarget(org.getId(), PolicyScopeType.ORGANIZATION)));
        priorityTargets.add(new PolicyTarget(companyId, PolicyScopeType.COMPANY));

        log.info("[탐색 우선순위] 총 대상 수={}, 대상 목록={}",
                priorityTargets.size(),
                priorityTargets.stream().map(t -> t.scopeType() + ":" + t.id()).collect(Collectors.toList()));

        // 3. 활성 정책 조회 (회사 ID로 필터링하여 멀티테넌트 보안 강화)
        List<UUID> targetIds = priorityTargets.stream().map(PolicyTarget::id).collect(Collectors.toList());
        List<PolicyAssignment> assignments = policyAssignmentRepository.findActiveAssignmentsByTargets(targetIds, companyId, LocalDateTime.now().toLocalDate());

        log.info("[활성 정책 조회] 조회된 정책 할당 수={}, 할당 목록={}",
                assignments.size(),
                assignments.stream().map(pa -> pa.getPolicy().getPolicyTypeCode() + ":" + pa.getScopeType() + ":" + pa.getTargetId()).collect(Collectors.toList()));

        // 4. 타입 필터링
        List<PolicyAssignment> filtered = assignments.stream()
                .filter(pa -> pa.getPolicy().getPolicyTypeCode() == typeCode)
                .collect(Collectors.toList());

        log.info("[타입 필터링] typeCode={} 필터링 후 정책 수={}", typeCode, filtered.size());

        // 5. 우선순위 순회 (scopeType + targetId 일치 시 즉시 반환)
        for (PolicyTarget target : priorityTargets) {
            for (PolicyAssignment pa : filtered) {
                if (pa.getScopeType() == target.scopeType() &&
                    pa.getTargetId().equals(target.id())) {
                    log.info("[정책 발견] policyId={}, policyName={}, scopeType={}, targetId={}",
                            pa.getPolicy().getId(), pa.getPolicy().getName(), pa.getScopeType(), pa.getTargetId());
                    return pa.getPolicy();
                }
            }
        }

        // 해당 타입의 정책이 없으면 null 반환
        log.warn("[정책 없음] memberId={}, typeCode={} - 할당된 정책을 찾을 수 없습니다", memberId, typeCode);
        return null;
    }

    /**
     * 정책 탐색을 위한 내부 레코드 (targetId + scopeType 쌍)
     */
    private record PolicyTarget(UUID id, PolicyScopeType scopeType) {}

    /**
     * 직원에 대한 유효 정책을 계층 구조 우선순위에 따라 조회합니다.
     * @deprecated 이 메서드는 첫 번째 정책만 반환하므로 사용을 권장하지 않습니다. findEffectivePolicyForMemberByType() 사용을 권장합니다.
     * 우선순위: 개인 > 직속 조직 > 상위 조직 ... > 회사
     * @param memberId 직원 ID
     * @param companyId 회사 ID
     * @param memberPositionId 직원의 직책 ID
     * @return 적용될 최종 정책
     */
    @Deprecated
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
        List<PolicyAssignment> assignments = policyAssignmentRepository.findActiveAssignmentsByTargets(priorityList, companyId, LocalDateTime.now().toLocalDate());

        // 2. [개인->조직->회사] 순서로 정책 탐색 (priorityList가 이미 우선순위 순서로 정렬됨)
        for (UUID targetId : priorityList) {
            for (PolicyAssignment assignment : assignments) {
                if (assignment.getTargetId().equals(targetId)) {
                    return assignment.getPolicy(); // 가장 먼저 찾은 정책을 반환
                }
            }
        }

        throw new ResourceNotFoundException("해당 직원에게 적용되는 유효한 근태 정책이 없습니다. 관리자에게 문의하세요.");
    }

    /**
     * [관리자용] 새로운 정책 할당을 생성합니다.
     */
    @Transactional
    public List<PolicyAssignmentResponse> createAssignments(UUID memberPositionId, UUID companyId, PolicyAssignmentRequest request) {
        checkPermission(memberPositionId, "attendance", "CREATE", "COMPANY");

        // 연차 정책은 회사 레벨에만 할당 가능 (법적 기준 + 성능 최적화)
        validateAnnualLeavePolicyScope(request);

        // 1. 요청된 할당들을 (policyId, scopeType)별로 그룹핑
        Map<String, List<PolicyAssignmentRequest.SingleAssignmentRequest>> groupedRequests = request.getAssignments().stream()
                .collect(Collectors.groupingBy(req -> req.getPolicyId() + "_" + req.getScopeType()));

        // 2. 각 그룹별로 이미 존재하는 할당 조회 후 중복 제거
        List<PolicyAssignment> newAssignments = new ArrayList<>();
        int duplicateCount = 0;

        for (Map.Entry<String, List<PolicyAssignmentRequest.SingleAssignmentRequest>> entry : groupedRequests.entrySet()) {
            List<PolicyAssignmentRequest.SingleAssignmentRequest> group = entry.getValue();
            if (group.isEmpty()) continue;

            UUID policyId = group.get(0).getPolicyId();
            PolicyScopeType scopeType = group.get(0).getScopeType();

            // COMPANY 타입인 경우 targetId를 헤더의 companyId로 통일
            List<UUID> targetIds;
            if (scopeType == PolicyScopeType.COMPANY) {
                targetIds = List.of(companyId); // 회사 타입은 항상 헤더의 companyId 사용
            } else {
                targetIds = group.stream().map(PolicyAssignmentRequest.SingleAssignmentRequest::getTargetId).collect(Collectors.toList());
            }

            // 이미 존재하는 할당 조회
            List<PolicyAssignment> existingAssignments = policyAssignmentRepository.findByPolicyIdAndTargetIdInAndScopeType(policyId, targetIds, scopeType);
            Set<UUID> existingTargetIds = existingAssignments.stream()
                    .map(PolicyAssignment::getTargetId)
                    .collect(Collectors.toSet());

            // 정책 조회
            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new ResourceNotFoundException("해당 정책을 찾을 수 없습니다. ID: " + policyId));

            // 중복되지 않은 할당만 생성
            for (PolicyAssignmentRequest.SingleAssignmentRequest singleRequest : group) {
                // COMPANY 타입이면 targetId를 companyId로 덮어씀
                UUID finalTargetId = (scopeType == PolicyScopeType.COMPANY) ? companyId : singleRequest.getTargetId();

                if (existingTargetIds.contains(finalTargetId)) {
                    duplicateCount++;
                    continue; // 이미 존재하는 할당은 스킵
                }

                newAssignments.add(PolicyAssignment.builder()
                        .policy(policy)
                        .scopeType(singleRequest.getScopeType())
                        .targetId(finalTargetId)
                        .assignedBy(memberPositionId)
                        .assignedAt(LocalDateTime.now())
                        .isActive(true)
                        .build());
            }
        }

        log.info("정책 할당 생성: 요청 {}건, 신규 생성 {}건, 중복 스킵 {}건",
                request.getAssignments().size(), newAssignments.size(), duplicateCount);

        List<PolicyAssignment> savedAssignments = policyAssignmentRepository.saveAll(newAssignments);

        // ✅ 정책 할당 생성 후 member_balance 생성
        log.info("=== member_balance 생성 시작: 총 {}건의 할당 처리 ===", savedAssignments.size());

        for (PolicyAssignment assignment : savedAssignments) {
            PolicyTypeCode typeCode = assignment.getPolicy().getPolicyTypeCode();
            String policyName = assignment.getPolicy().getName();

            // 잔액 차감 필요한 휴가 정책만 처리
            if (!typeCode.isBalanceDeductible()) {
                log.debug("스킵: {} ({}) - 근로시간 관련 정책 (잔액 차감 불필요)", policyName, typeCode.getCodeName());
                continue; // 근로시간 관련 정책은 스킵
            }

            // COMPANY 레벨 할당만 처리 (연차는 이미 검증됨)
            if (assignment.getScopeType() != PolicyScopeType.COMPANY) {
                log.warn("스킵: {} ({}) - COMPANY 레벨이 아님 (scopeType={})",
                    policyName, typeCode.getCodeName(), assignment.getScopeType());
                continue;
            }

            log.info(">>> 처리 시작: {} ({}) - companyId={}", policyName, typeCode.getCodeName(), companyId);

            try {
                switch (typeCode) {
                    case ANNUAL_LEAVE:
                        log.info("  → 연차 정책: 전체 직원 초기 연차 부여 시작");
                        annualLeaveAccrualService.grantInitialAnnualLeaveForAllMembers(
                            companyId,
                            java.time.LocalDate.now()
                        );
                        log.info("  ✅ 연차 정책: 부여 완료");
                        break;

                    case MATERNITY_LEAVE:
                    case PATERNITY_LEAVE:
                    case MENSTRUAL_LEAVE:
                    case CHILDCARE_LEAVE:
                    case FAMILY_CARE_LEAVE:
                        // defaultDays가 있으면 해당 일수만큼 부여, 없으면 0일로 시작
                        Double defaultDays = null;
                        if (assignment.getPolicy().getRuleDetails() != null
                            && assignment.getPolicy().getRuleDetails().getLeaveRule() != null) {
                            defaultDays = assignment.getPolicy().getRuleDetails().getLeaveRule().getDefaultDays();
                        }

                        log.info("  → {} 정책: defaultDays={}", typeCode.getCodeName(), defaultDays);

                        if (defaultDays != null && defaultDays > 0) {
                            log.info("  → 고정 일수 부여 모드: {}일 부여", defaultDays);
                            grantFixedLeaveDays(companyId, typeCode, assignment.getPolicy());
                            log.info("  ✅ 고정 일수 부여 완료: {}일", defaultDays);
                        } else {
                            log.info("  → 0일 시작 모드: 관리자 개별 부여 방식");
                            createZeroBalanceForAllMembers(companyId, typeCode, assignment.getPolicy());
                            log.info("  ✅ 0일 잔액 생성 완료");
                        }
                        break;

                    default:
                        log.warn("  ⚠️ 처리되지 않은 정책 타입: {}", typeCode);
                        break;
                }
            } catch (Exception e) {
                log.error("  ❌ member_balance 생성 실패: {} ({})", policyName, typeCode.getCodeName(), e);
                // 한 정책이 실패해도 다른 정책은 계속 처리
            }
        }

        log.info("=== member_balance 생성 완료 ===");

        return savedAssignments.stream()
                .map(PolicyAssignmentResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * [관리자용] 특정 회사의 모든 정책 할당 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<PolicyAssignmentResponse> findAssignments(UUID memberId, UUID memberPositionId, UUID companyId, Pageable pageable) {
        Page<PolicyAssignment> assignmentsPage = policyAssignmentRepository.findAllByPolicy_CompanyId(companyId, pageable);

        // 1. 직원 정보 조회를 위해 ID 추출
        List<UUID> memberIds = assignmentsPage.getContent().stream()
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

        // 2. 회사의 전체 조직도 정보 조회
        final Map<UUID, String> orgNameMap = new HashMap<>();
        final Map<UUID, UUID> childToParentMap = new HashMap<>();
        final OrganizationNodeDto companyInfo;

        try {
            ApiResponse<List<OrganizationNodeDto>> response = memberClient.getOrganization(memberId);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                companyInfo = response.getData().get(0); // 최상위 노드가 회사

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

        // 3. Page.map을 사용하여 DTO로 변환 및 정보 채우기
        return assignmentsPage.map(assignment -> {
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
        });
    }

    @Transactional
    public void deleteAssignment(UUID memberPositionId, UUID assignmentId) {
        checkPermission(memberPositionId, "attendance", "DELETE", "COMPANY");

        PolicyAssignment assignment = policyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 정책 할당을 찾을 수 없습니다. ID: " + assignmentId));
        
        policyAssignmentRepository.delete(assignment);
    }

    /**
     * [관리자용] 특정 대상(target)에 대한 정책 할당 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<PolicyAssignmentResponse> findPolicyAssignmentsByTarget(UUID memberPositionId, UUID targetId, PolicyScopeType scopeType) {
        checkPermission(memberPositionId, "attendance", "READ", "COMPANY");
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
        checkPermission(memberPositionId, "attendance", "UPDATE", "COMPANY");
        PolicyAssignment assignment = policyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 정책 할당을 찾을 수 없습니다. ID: " + assignmentId));

        assignment.deactivate();
    }

    /**
     * [관리자용] 여러 정책 할당을 일괄 해지(비활성화)합니다.
     * member_balance는 유지하되 사용 불가 처리
     */
    @Transactional
    public void revokeAssignments(UUID memberPositionId, List<UUID> assignmentIds) {
        checkPermission(memberPositionId, "attendance", "UPDATE", "COMPANY");
        List<PolicyAssignment> assignmentsToRevoke = policyAssignmentRepository.findAllById(assignmentIds);
        if (assignmentsToRevoke.size() != assignmentIds.size()) {
            throw new BusinessException("요청된 ID 목록에 존재하지 않는 할당이 포함되어 있습니다.");
        }

        // 정책 할당 비활성화
        assignmentsToRevoke.forEach(PolicyAssignment::deactivate);

        // ✅ 연차 정책인 경우 member_balance 사용 불가 처리
        for (PolicyAssignment assignment : assignmentsToRevoke) {
            if (assignment.getPolicy().getPolicyTypeCode().isBalanceDeductible()) {
                suspendMemberBalancesForPolicy(
                        assignment.getPolicy().getCompanyId(),
                        assignment.getPolicy().getPolicyTypeCode()
                );
            }
        }
    }

    /**
     * [관리자용] 여러 정책 할당을 일괄 재활성화합니다.
     * member_balance도 다시 사용 가능 처리
     */
    @Transactional
    public void reactivateAssignments(UUID memberPositionId, List<UUID> assignmentIds) {
        checkPermission(memberPositionId, "attendance", "UPDATE", "COMPANY");
        List<PolicyAssignment> assignmentsToReactivate = policyAssignmentRepository.findAllById(assignmentIds);
        if (assignmentsToReactivate.size() != assignmentIds.size()) {
            throw new BusinessException("요청된 ID 목록에 존재하지 않는 할당이 포함되어 있습니다.");
        }

        // 정책 할당 재활성화
        assignmentsToReactivate.forEach(PolicyAssignment::activate);

        // ✅ 연차 정책인 경우 member_balance 재활성화
        for (PolicyAssignment assignment : assignmentsToReactivate) {
            if (assignment.getPolicy().getPolicyTypeCode().isBalanceDeductible()) {
                activateMemberBalancesForPolicy(
                        assignment.getPolicy().getCompanyId(),
                        assignment.getPolicy().getPolicyTypeCode()
                );
            }
        }
    }

    /**
     * [관리자용] 여러 정책 할당을 일괄 삭제합니다.
     * 사용 내역이 있으면 삭제 불가, member_balance도 함께 삭제
     */
    @Transactional
    public void deleteAssignments(UUID memberPositionId, List<UUID> assignmentIds) {
        checkPermission(memberPositionId, "attendance", "DELETE", "COMPANY");

        List<PolicyAssignment> assignmentsToDelete = policyAssignmentRepository.findAllById(assignmentIds);
        if (assignmentsToDelete.isEmpty()) {
            return;
        }

        // ✅ 삭제 전 사용 내역 검증 및 member_balance 삭제
        for (PolicyAssignment assignment : assignmentsToDelete) {
            if (assignment.getPolicy().getPolicyTypeCode().isBalanceDeductible()) {
                // 1. 사용 내역 확인
                validateNoUsageBeforeDelete(
                        assignment.getPolicy().getCompanyId(),
                        assignment.getPolicy().getPolicyTypeCode()
                );

                // 2. member_balance 완전 삭제
                deleteMemberBalancesForPolicy(
                        assignment.getPolicy().getCompanyId(),
                        assignment.getPolicy().getPolicyTypeCode()
                );
            }
        }

        // 3. 정책 할당 삭제
        policyAssignmentRepository.deleteAllById(assignmentIds);
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
     * 직원에게 할당된 모든 활성 정책을 조회합니다.
     * 계층 구조 우선순위: 직책(MEMBER_POSITION) > 개인(MEMBER) > 조직(ORGANIZATION) > 상위 조직 ... > 회사(COMPANY)
     * 같은 타입의 정책이 여러 레벨에 할당되어 있으면 우선순위가 높은 것만 반환합니다.
     *
     * @param memberId 직원 ID
     * @param memberPositionId 직원의 직책 ID
     * @param companyId 회사 ID
     * @return 할당된 정책 목록 (타입별로 중복 제거된 최우선 정책만 포함)
     */
    @Transactional(readOnly = true)
    public List<Policy> findAllAssignedPoliciesForMember(UUID memberId, UUID memberPositionId, UUID companyId) {
        // 1. 조직 계층 조회
        List<OrganizationRes> orgPath = new ArrayList<>();
        try {
            ApiResponse<List<OrganizationRes>> response = memberClient.getOrganizationList(memberPositionId);
            if (response != null && response.getData() != null) {
                orgPath = response.getData();
            }
        } catch (Exception e) {
            throw new BusinessException("Member-service에서 조직 정보를 가져오는 데 실패했습니다.", e);
        }

        // 2. 탐색 우선순위 구성
        List<PolicyTarget> priorityTargets = new ArrayList<>();
        priorityTargets.add(new PolicyTarget(memberPositionId, PolicyScopeType.MEMBER_POSITION));
        priorityTargets.add(new PolicyTarget(memberId, PolicyScopeType.MEMBER));
        orgPath.forEach(org -> priorityTargets.add(new PolicyTarget(org.getId(), PolicyScopeType.ORGANIZATION)));
        priorityTargets.add(new PolicyTarget(companyId, PolicyScopeType.COMPANY));

        // 3. 활성 정책 조회
        List<UUID> targetIds = priorityTargets.stream().map(PolicyTarget::id).collect(Collectors.toList());
        List<PolicyAssignment> assignments = policyAssignmentRepository.findActiveAssignmentsByTargets(
                targetIds, companyId, LocalDateTime.now().toLocalDate());

        // 4. 타입별로 최우선 정책만 선택
        Map<PolicyTypeCode, Policy> policyByType = new LinkedHashMap<>();
        for (PolicyTarget target : priorityTargets) {
            for (PolicyAssignment pa : assignments) {
                if (pa.getScopeType() == target.scopeType() &&
                    pa.getTargetId().equals(target.id())) {
                    PolicyTypeCode typeCode = pa.getPolicy().getPolicyTypeCode();
                    // 아직 이 타입의 정책이 선택되지 않았으면 추가 (우선순위 높은 것만)
                    policyByType.putIfAbsent(typeCode, pa.getPolicy());
                }
            }
        }

        return new ArrayList<>(policyByType.values());
    }

    /**
     * 연차 정책 할당 검증
     * 연차 정책은 근로기준법에 따라 회사 단위로 동일하게 적용되어야 하므로
     * 조직, 개인별 차등 할당을 방지하고 회사 레벨에만 할당 가능하도록 제한
     *
     * 이점:
     * - 법적 기준 준수 (근로기준법 제60조)
     * - 배치 작업 성능 최적화 (N번 정책 조회 → 1번 조회)
     * - 정책 관리 단순화
     *
     * @param request 정책 할당 요청
     * @throws BusinessException 연차 정책을 회사 레벨이 아닌 곳에 할당하려고 할 경우
     */
    private void validateAnnualLeavePolicyScope(PolicyAssignmentRequest request) {
        for (PolicyAssignmentRequest.SingleAssignmentRequest assignment : request.getAssignments()) {
            // 정책 조회
            Policy policy = policyRepository.findById(assignment.getPolicyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "해당 정책을 찾을 수 없습니다. ID: " + assignment.getPolicyId()));

            // 연차 정책인지 확인
            if (policy.getPolicyTypeCode() == PolicyTypeCode.ANNUAL_LEAVE) {
                // 회사 레벨이 아니면 예외 발생
                if (assignment.getScopeType() != PolicyScopeType.COMPANY) {
                    throw new BusinessException(
                            "연차 정책은 회사 전체에만 할당 가능합니다. " +
                            "근로기준법에 따라 같은 회사 내 모든 직원에게 동일한 연차 규정이 적용되어야 합니다. " +
                            "(정책명: " + policy.getName() + ", 시도한 할당 레벨: " + assignment.getScopeType() + ")"
                    );
                }

                log.info("연차 정책 검증 통과: policyId={}, policyName={}, scopeType=COMPANY",
                        policy.getId(), policy.getName());
            }
        }
    }

    // ========== member_balance 관리 Helper Methods ==========

    /**
     * 고정 일수 휴가 부여 (출산전후휴가, 배우자 출산휴가, 생리휴가)
     */
    private void grantFixedLeaveDays(UUID companyId, PolicyTypeCode typeCode, Policy policy) {
        log.info("    [grantFixedLeaveDays] 시작: typeCode={}, companyId={}", typeCode, companyId);

        // 내부 전용 API 사용 (권한 체크 없음 - 시스템 작업용)
        List<com.crewvy.workforce_service.feignClient.dto.response.MemberEmploymentInfoDto> members =
                memberClient.getEmploymentInfoInternal(companyId)
                        .getData();

        log.info("    [grantFixedLeaveDays] 회사 전체 직원 조회 완료: {}명", members != null ? members.size() : 0);

        Double defaultDays = policy.getRuleDetails().getLeaveRule().getDefaultDays();
        if (defaultDays == null) {
            defaultDays = 0.0;
        }

        log.info("    [grantFixedLeaveDays] 부여할 일수: {}일", defaultDays);

        int currentYear = java.time.LocalDate.now().getYear();
        List<com.crewvy.workforce_service.attendance.entity.MemberBalance> balancesToSave = new ArrayList<>();
        int workingMemberCount = 0;
        int existingBalanceCount = 0;

        for (com.crewvy.workforce_service.feignClient.dto.response.MemberEmploymentInfoDto member : members) {
            if ("MS001".equals(member.getMemberStatus())) { // MS001 = WORKING (재직)
                workingMemberCount++;
                Optional<com.crewvy.workforce_service.attendance.entity.MemberBalance> existing =
                        memberBalanceRepository.findByMemberIdAndBalanceTypeCodeAndYear(
                                member.getMemberId(), typeCode, currentYear
                        );

                if (existing.isEmpty()) {
                    com.crewvy.workforce_service.attendance.entity.MemberBalance balance =
                            com.crewvy.workforce_service.attendance.entity.MemberBalance.builder()
                                    .memberId(member.getMemberId())
                                    .companyId(companyId)
                                    .balanceTypeCode(typeCode)
                                    .year(currentYear)
                                    .totalGranted(defaultDays)
                                    .totalUsed(0.0)
                                    .remaining(defaultDays)
                                    .expirationDate(java.time.LocalDate.of(currentYear, 12, 31))
                                    .isPaid(policy.getIsPaid())
                                    .isUsable(true)
                                    .build();
                    balancesToSave.add(balance);
                } else {
                    existingBalanceCount++;
                }
            }
        }

        log.info("    [grantFixedLeaveDays] 재직 중인 직원: {}명, 기존 잔액 보유: {}명, 신규 생성 대상: {}명",
                workingMemberCount, existingBalanceCount, balancesToSave.size());

        if (!balancesToSave.isEmpty()) {
            memberBalanceRepository.saveAll(balancesToSave);
            log.info("    [grantFixedLeaveDays] ✅ DB 저장 완료: {}건 ({}일씩 부여)", balancesToSave.size(), defaultDays);
        } else {
            log.info("    [grantFixedLeaveDays] ⚠️ 생성할 잔액 없음 (모두 이미 존재하거나 재직자 없음)");
        }
    }

    /**
     * 0일 잔액 생성 (육아휴직, 가족돌봄휴가 - 관리자 개별 부여)
     */
    private void createZeroBalanceForAllMembers(UUID companyId, PolicyTypeCode typeCode, Policy policy) {
        log.info("    [createZeroBalanceForAllMembers] 시작: typeCode={}, companyId={}", typeCode, companyId);

        // 내부 전용 API 사용 (권한 체크 없음 - 시스템 작업용)
        List<com.crewvy.workforce_service.feignClient.dto.response.MemberEmploymentInfoDto> members =
                memberClient.getEmploymentInfoInternal(companyId)
                        .getData();

        log.info("    [createZeroBalanceForAllMembers] 회사 전체 직원 조회 완료: {}명", members != null ? members.size() : 0);

        int currentYear = java.time.LocalDate.now().getYear();
        List<com.crewvy.workforce_service.attendance.entity.MemberBalance> balancesToSave = new ArrayList<>();
        int workingMemberCount = 0;
        int existingBalanceCount = 0;

        for (com.crewvy.workforce_service.feignClient.dto.response.MemberEmploymentInfoDto member : members) {
            if ("MS001".equals(member.getMemberStatus())) { // MS001 = WORKING (재직)
                workingMemberCount++;
                Optional<com.crewvy.workforce_service.attendance.entity.MemberBalance> existing =
                        memberBalanceRepository.findByMemberIdAndBalanceTypeCodeAndYear(
                                member.getMemberId(), typeCode, currentYear
                        );

                if (existing.isEmpty()) {
                    com.crewvy.workforce_service.attendance.entity.MemberBalance balance =
                            com.crewvy.workforce_service.attendance.entity.MemberBalance.builder()
                                    .memberId(member.getMemberId())
                                    .companyId(companyId)
                                    .balanceTypeCode(typeCode)
                                    .year(currentYear)
                                    .totalGranted(0.0)
                                    .totalUsed(0.0)
                                    .remaining(0.0)
                                    .expirationDate(null) // 만료 없음
                                    .isPaid(policy.getIsPaid())
                                    .isUsable(true)
                                    .build();
                    balancesToSave.add(balance);
                } else {
                    existingBalanceCount++;
                }
            }
        }

        log.info("    [createZeroBalanceForAllMembers] 재직 중인 직원: {}명, 기존 잔액 보유: {}명, 신규 생성 대상: {}명",
                workingMemberCount, existingBalanceCount, balancesToSave.size());

        if (!balancesToSave.isEmpty()) {
            memberBalanceRepository.saveAll(balancesToSave);
            log.info("    [createZeroBalanceForAllMembers] ✅ DB 저장 완료: {}건 (0일로 시작, 관리자 개별 부여)", balancesToSave.size());
        } else {
            log.info("    [createZeroBalanceForAllMembers] ⚠️ 생성할 잔액 없음 (모두 이미 존재하거나 재직자 없음)");
        }
    }

    /**
     * 삭제 전 사용 내역 검증
     * 이미 사용한 휴가가 있으면 예외 발생
     */
    private void validateNoUsageBeforeDelete(UUID companyId, PolicyTypeCode typeCode) {
        int currentYear = java.time.LocalDate.now().getYear();
        List<com.crewvy.workforce_service.attendance.entity.MemberBalance> balances =
                memberBalanceRepository.findByCompanyIdAndBalanceTypeCodeAndYear(companyId, typeCode, currentYear);

        boolean hasUsage = balances.stream()
                .anyMatch(balance -> balance.getTotalUsed() != null && balance.getTotalUsed() > 0);

        if (hasUsage) {
            throw new BusinessException(
                    "이미 사용한 휴가가 있는 직원이 있어 정책 할당을 삭제할 수 없습니다. " +
                            "정책을 중단하려면 '비활성화'를 사용하세요."
            );
        }
    }

    /**
     * 회사 전체 직원의 member_balance 완전 삭제
     */
    private void deleteMemberBalancesForPolicy(UUID companyId, PolicyTypeCode typeCode) {
        int currentYear = java.time.LocalDate.now().getYear();
        List<com.crewvy.workforce_service.attendance.entity.MemberBalance> balances =
                memberBalanceRepository.findByCompanyIdAndBalanceTypeCodeAndYear(companyId, typeCode, currentYear);

        memberBalanceRepository.deleteAll(balances);

        log.info("정책 할당 삭제로 인한 member_balance 삭제: companyId={}, typeCode={}, count={}",
                companyId, typeCode, balances.size());
    }

    /**
     * 회사 전체 직원의 member_balance 사용 불가 처리
     */
    private void suspendMemberBalancesForPolicy(UUID companyId, PolicyTypeCode typeCode) {
        int currentYear = java.time.LocalDate.now().getYear();
        List<com.crewvy.workforce_service.attendance.entity.MemberBalance> balances =
                memberBalanceRepository.findByCompanyIdAndBalanceTypeCodeAndYear(companyId, typeCode, currentYear);

        balances.forEach(com.crewvy.workforce_service.attendance.entity.MemberBalance::suspend);

        log.info("정책 할당 비활성화로 인한 member_balance 중단: companyId={}, typeCode={}, count={}",
                companyId, typeCode, balances.size());
    }

    /**
     * 회사 전체 직원의 member_balance 재활성화
     */
    private void activateMemberBalancesForPolicy(UUID companyId, PolicyTypeCode typeCode) {
        int currentYear = java.time.LocalDate.now().getYear();
        List<com.crewvy.workforce_service.attendance.entity.MemberBalance> balances =
                memberBalanceRepository.findByCompanyIdAndBalanceTypeCodeAndYear(companyId, typeCode, currentYear);

        balances.forEach(com.crewvy.workforce_service.attendance.entity.MemberBalance::activate);

        log.info("정책 할당 재활성화로 인한 member_balance 재개: companyId={}, typeCode={}, count={}",
                companyId, typeCode, balances.size());
    }
}
