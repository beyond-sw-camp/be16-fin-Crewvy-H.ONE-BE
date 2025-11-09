package com.crewvy.workforce_service.approval.service;

import com.crewvy.common.S3.S3Uploader;
import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.common.dto.ScheduleDto;
import com.crewvy.common.entity.Bool;
import com.crewvy.common.event.ApprovalCompletedEvent;
import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.SerializationException;
import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.constant.LineStatus;
import com.crewvy.workforce_service.approval.constant.RequirementType;
import com.crewvy.workforce_service.approval.dto.request.*;
import com.crewvy.workforce_service.approval.dto.response.*;
import com.crewvy.workforce_service.approval.entity.*;
import com.crewvy.workforce_service.approval.repository.*;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.entity.Request;
import com.crewvy.workforce_service.attendance.event.AttendanceRequestApprovedEvent;
import com.crewvy.workforce_service.attendance.repository.RequestRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.MemberDto;
import com.crewvy.workforce_service.feignClient.dto.response.OrganizationNodeDto;
import com.crewvy.workforce_service.feignClient.dto.response.PositionDto;
import com.crewvy.workforce_service.feignClient.dto.response.TitleRes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalService {
    private final ApprovalRepository approvalRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final ApprovalReplyRepository approvalReplyRepository;
    private final ApprovalSearchOutboxEventRepository approvalSearchOutboxEventRepository;
    private final S3Uploader s3Uploader;
    private final MemberClient memberClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final RequestRepository requestRepository;
    private final ApprovalPolicyRepository approvalPolicyRepository;

//    문서 양식 생성
    public UUID uploadDocument(UploadDocumentDto dto) {
        ApprovalDocument newDocument = ApprovalDocument.builder()
                .documentName(dto.getDocumentName())
                .metadata(dto.getMetadata())
                .build();
        approvalDocumentRepository.save(newDocument);

        return newDocument.getId();
    }

//    문서 양식 조회
    @Transactional(readOnly = true)
    public DocumentResponseDto getDocument(UUID id, UUID requestId, UUID memberPositionId, UUID memberId) {
        // 1. 문서와 정책 목록을 한 번에 조회합니다 (N+1 방지).
        ApprovalDocument document = approvalDocumentRepository.findByIdWithPolicies(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문서입니다."));

        Request request = null;
        if(requestId != null) {
            request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 요청입니다."));
        }

        // 2. 조직도는 한 번만 조회하여 재사용합니다.
        List<OrganizationNodeDto> orgTree = memberClient.getOrganization(memberId).getData();

        // 3. 각 정책을 해석하여 '순서(lineIndex)'와 '찾아야 할 결재자 ID'를 Pair로 묶어 저장합니다.
        List<Pair<Integer, UUID>> resolvedPolicies = new ArrayList<>();
        for (ApprovalPolicy policy : document.getPolicyList()) {
            // 1. null로 즉시 초기화 (컴파일 에러 해결)
            UUID approverId;

            if (policy.getRequirementType() == RequirementType.TITLE) {
                Optional<UUID> approverIdOptional = findApproverByTitle(orgTree, memberPositionId, policy.getRequirementId());

                approverId = approverIdOptional.orElse(null);
            } else {
                approverId = policy.getRequirementId();
            }

            // 3. approverId가 null이 아닌 경우 (값을 찾았거나, else 블록을 탄 경우)
            if (approverId != null) {
                resolvedPolicies.add(Pair.of(policy.getLineIndex(), approverId));
            }
        }

        // 4. 수집된 모든 결재자 ID로 FeignClient를 딱 한 번 호출하여 상세 정보를 가져옵니다.
        List<UUID> allApproverIds = resolvedPolicies.stream().map(Pair::getSecond).toList();
        Map<UUID, PositionDto> positionMap = new HashMap<>();
        if (!allApproverIds.isEmpty()) {
            List<PositionDto> positions = memberClient.getPositionList(memberPositionId, new IdListReq(allApproverIds)).getData();
            if (positions != null) {
                positionMap = positions.stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, pos -> pos));
            }
        }

        // 5. 💡 조회된 정보를 조합하여 최종 'ApprovalStepDto' 리스트를 생성합니다. (핵심 요리 과정)
        final Map<UUID, PositionDto> finalPositionMap = positionMap;
        List<ApprovalStepDto> policyLine = resolvedPolicies.stream()
                .map(pair -> {
                    int lineIndex = pair.getFirst();
                    UUID approverId = pair.getSecond();
                    PositionDto position = finalPositionMap.get(approverId);

                    if (position == null) return null; // 상세 정보를 찾지 못한 경우

                    // ApprovalStepDto를 빌드합니다.
                    return ApprovalStepDto.builder()
                            .index(lineIndex)
                            .approverId(position.getMemberPositionId())
                            .approverName(position.getMemberName())
                            .approverPosition(position.getTitleName())
                            .approverOrganization(position.getOrganizationName())
                            .build();
                })
                .filter(Objects::nonNull) // null인 경우 최종 리스트에서 제외
                .sorted(Comparator.comparing(ApprovalStepDto::getIndex)) // lineIndex 순서대로 최종 정렬
                .toList();

        RequestResDto dto = null;
        if(request != null) {
            dto = RequestResDto.builder()
                    .requestType(request.getPolicy().getPolicyTypeCode().getCodeName())
                    .requestUnit(request.getRequestUnit().getCodeName())
                    .startDate(request.getStartDateTime())
                    .endDate(request.getEndDateTime())
                    .reason(request.getReason())
                    .workLocation(request.getWorkLocation())
                    .build();
        }

        // 6. 완성된 추천 결재자 목록을 최종 DTO에 담아 반환합니다.
        return DocumentResponseDto.builder()
                .documentId(document.getId())
                .documentName(document.getDocumentName())
                .metadata(document.getMetadata())
                .policy(policyLine)
                .request(dto)
                .build();
    }

    private Optional<UUID> findApproverByTitle(List<OrganizationNodeDto> orgTree, UUID myMemberPositionId, UUID requiredTitleId) {
        // 1. '나'의 위치와 경로 찾기 (동일)
        List<OrganizationNodeDto> pathToMe = findPathToMember(orgTree, myMemberPositionId);

        if (pathToMe == null || pathToMe.isEmpty()) {
            throw new EntityNotFoundException("요청자를 조직도에서 찾을 수 없습니다."); // 이것은 유지 (로직 수행 전제조건)
        }

        // 2. '나'의 조직부터 상위로 올라가며 탐색 (동일)
        for (int i = pathToMe.size() - 1; i >= 0; i--) {
            OrganizationNodeDto currentOrg = pathToMe.get(i);

            // 3. 현재 조직에서 직책이 일치하는 멤버 찾기 (동일)
            Optional<MemberDto> foundApprover = currentOrg.getMembers().stream()
                    .filter(member -> requiredTitleId.equals(member.getTitleId()))
                    .findFirst();

            if (foundApprover.isPresent()) {
                // 4. 찾았으면 Optional.of(...)로 감싸서 반환
                return Optional.of(foundApprover.get().getMemberPositionId());
            }
        }

        // 5. 최상위까지 못 찾은 경우, 예외 대신 Optional.empty() 반환
        return Optional.empty();
    }

    private List<OrganizationNodeDto> findPathToMember(List<OrganizationNodeDto> nodes, UUID targetMemberPositionId) {
        for (OrganizationNodeDto node : nodes) {
            // 현재 노드의 멤버 목록에 타겟이 있는지 확인
            boolean memberExists = node.getMembers().stream() // .members() -> .getMembers()
                    .anyMatch(member -> member.getMemberPositionId().equals(targetMemberPositionId)); // .memberPositionId() -> .getMemberPositionId()

            if (memberExists) {
                // 찾았다! 현재 노드를 포함하는 새로운 경로를 생성하여 반환
                List<OrganizationNodeDto> path = new ArrayList<>();
                path.add(node);
                return path;
            }

            // 하위 조직(children)으로 더 깊이 들어가서 재귀적으로 탐색
            if (node.getChildren() != null && !node.getChildren().isEmpty()) { // .children() -> .getChildren()
                List<OrganizationNodeDto> pathFromChild = findPathToMember(node.getChildren(), targetMemberPositionId); // .children() -> .getChildren()

                if (pathFromChild != null) {
                    // 하위 조직에서 경로를 찾았다면, 현재 노드를 경로의 맨 앞에 추가하여 위로 전달
                    pathFromChild.add(0, node);
                    return pathFromChild;
                }
            }
        }
        // 이 레벨에서 못 찾았으면 null 반환
        return null;
    }

//    문서 양식 리스트 전체 조회
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getDocumentList() {
        List<ApprovalDocument> documentList = approvalDocumentRepository.findAll();
        List<DocumentResponseDto> dtoList = new ArrayList<>();
        for(ApprovalDocument a : documentList) {
            DocumentResponseDto dto = DocumentResponseDto.builder()
                    .documentId(a.getId())
                    .documentName(a.getDocumentName())
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

//    문서 양식 리스트 조회
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getDocumentDirectList() {
        List<ApprovalDocument> documentList = approvalDocumentRepository.findByIsDirectCreatable(Bool.TRUE);
        List<DocumentResponseDto> dtoList = new ArrayList<>();
        for(ApprovalDocument a : documentList) {
            DocumentResponseDto dto = DocumentResponseDto.builder()
                    .documentId(a.getId())
                    .documentName(a.getDocumentName())
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

//    결재 생성
    public UUID createApproval(CreateApprovalDto dto, UUID memberPositionId) {
        Approval approval = null;
        if(dto.getApprovalId() == null) {
            // 1. 결재 문서(부모) 생성
            ApprovalDocument document = approvalDocumentRepository.findById(dto.getDocumentId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문서입니다."));
            approval = Approval.builder()
                    .approvalDocument(document)
                    .title(dto.getTitle())
                    .contents(dto.getContents())
                    .state(ApprovalState.PENDING) // 💡 우선 '진행중'으로 설정
                    .memberPositionId(memberPositionId)
                    .build();
        }
        else {
            approval = approvalRepository.findById(dto.getApprovalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재입니다."));
            approval.updateApproval(dto.getTitle(), dto.getContents());
            approval.updateState(ApprovalState.PENDING);
            approval.getApprovalLineList().clear();
        }
        // 2. 결재 라인 정렬
        dto.getLineDtoList().sort(Comparator.comparing(ApprovalLineRequestDto::getLineIndex));
        UUID alarmId = null;

        // 3. 결재 라인(자식) 생성
        for (ApprovalLineRequestDto alDto : dto.getLineDtoList()) {
            LineStatus currentStatus;
            LocalDateTime approvalDate = null;

            if (alDto.getLineIndex() == 1) {
                currentStatus = LineStatus.APPROVED;
                approvalDate = LocalDateTime.now();
            } else if (alDto.getLineIndex() == 2) {
                currentStatus = LineStatus.PENDING;
                List<PositionDto> position =  memberClient.getPositionList(memberPositionId,
                        new IdListReq(List.of(alDto.getMemberPositionId()))).getData();
                alarmId = position.get(0).getMemberId();
            } else {
                currentStatus = LineStatus.WAITING;
            }



            ApprovalLine approvalLine = ApprovalLine.builder()
                    .approval(approval)
                    .memberPositionId(alDto.getMemberPositionId())
                    .lineIndex(alDto.getLineIndex())
                    .lineStatus(currentStatus)
                    .approvalDate(approvalDate)
                    .build();

            approval.getApprovalLineList().add(approvalLine);
        }

        // 4. 최종 상태 결정: 결재 라인이 1명뿐인 경우 최종 승인 처리
        if (dto.getLineDtoList().size() == 1) {
            approval.updateState(ApprovalState.APPROVED);
        }

        // 5. 부모 엔티티를 한 번만 저장
        Approval savedApproval = approvalRepository.saveAndFlush(approval);

//        request에 approvalId 추가
        if(dto.getRequestId() != null) {
            Request request = requestRepository.findById(dto.getRequestId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 신청입니다."));

            request.updateApprovalId(approval.getId());
            requestRepository.save(request); // approval_id를 DB에 반영

            // 근태 Request 승인 이벤트 발행 (AttendanceService에서 처리)
            if(approval.getState().equals(ApprovalState.APPROVED) || approval.getState().equals(ApprovalState.REJECTED)) {
                AttendanceRequestApprovedEvent attendanceEvent = new AttendanceRequestApprovedEvent(
                        request.getId(),
                        approval.getId(),
                        approval.getState(),
                        LocalDateTime.now()
                );
                eventPublisher.publishEvent(attendanceEvent);
                log.info("근태 Request 승인 이벤트 발행: requestId={}, approvalState={}",
                        request.getId(), approval.getState());
            }

            // 일정 등록 이벤트 발행 (출장/휴가)
            if(approval.getState().equals(ApprovalState.APPROVED)) {
                String policyTypeName = request.getPolicy().getPolicyTypeCode().getCodeName();
                if(policyTypeName.equals("출장")) {
                    ScheduleDto schedule = ScheduleDto.builder()
                            .originId(request.getId())
                            .type("CT004")
                            .title(policyTypeName)
                            .contents(request.getReason())
                            .startDate(request.getStartDateTime())
                            .endDate(request.getEndDateTime())
                            .memberId(request.getMemberId())
                            .build();
                    eventPublisher.publishEvent(schedule);
                }
                else if(policyTypeName.contains("휴가")) {
                    ScheduleDto schedule = ScheduleDto.builder()
                            .originId(request.getId())
                            .type("CT003")
                            .title(request.getRequestUnit().getCodeName())
                            .contents(request.getReason())
                            .startDate(request.getStartDateTime())
                            .endDate(request.getEndDateTime())
                            .memberId(request.getMemberId())
                            .build();
                    eventPublisher.publishEvent(schedule);
                }
            }
        }


//        알림 전송
        if(alarmId != null) {
            NotificationMessage message = NotificationMessage.builder()
                    .memberId(alarmId)
                    .notificationType("NT004")
                    .content("전자결재 : " + approval.getTitle() + " 문서가  도착했습니다.")
                    .targetId(approval.getId())
                    .build();

            eventPublisher.publishEvent(message);
        }

        // Elasticsearch 저장을 위한 이벤트 발행
        List<PositionDto> requesterPositionInfo = memberClient.getPositionList(memberPositionId, new IdListReq(List.of(savedApproval.getMemberPositionId()))).getData();
        if (requesterPositionInfo != null && !requesterPositionInfo.isEmpty()) {
            List<String> approverIdList = savedApproval.getApprovalLineList().stream()
                    .map(line -> line.getMemberPositionId().toString())
                    .collect(Collectors.toList());

            ApprovalCompletedEvent approvalEvent = new ApprovalCompletedEvent(
                    savedApproval.getId(),
                    memberPositionId,
                    savedApproval.getTitle(),
                    requesterPositionInfo.get(0).getTitleName(),
                    requesterPositionInfo.get(0).getMemberName(),
                    approverIdList,
                    savedApproval.getCreatedAt()
            );
            eventPublisher.publishEvent(approvalEvent);
        }

        return approval.getId();
    }

//    결재 임시저장
    public UUID draftApproval(CreateApprovalDto dto, UUID memberPositionId) {
        Approval approval = null;
        if(dto.getApprovalId() == null) {
            ApprovalDocument document = approvalDocumentRepository.findById(dto.getDocumentId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문서입니다."));
            approval = Approval.builder()
                    .approvalDocument(document)
                    .title(dto.getTitle())
                    .contents(dto.getContents())
                    .state(ApprovalState.DRAFT)
                    .memberPositionId(memberPositionId)
                    .build();
        }
        else {
            approval = approvalRepository.findById(dto.getApprovalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재입니다."));
            approval.updateApproval(dto.getTitle(), dto.getContents());
            approval.getApprovalLineList().clear();
        }

        approvalRepository.save(approval);

        // 2. 결재 라인 정렬
        dto.getLineDtoList().sort(Comparator.comparing(ApprovalLineRequestDto::getLineIndex));

        // 3. 결재 라인(자식) 생성
        for (ApprovalLineRequestDto alDto : dto.getLineDtoList()) {
            ApprovalLine approvalLine = ApprovalLine.builder()
                    .approval(approval)
                    .memberPositionId(alDto.getMemberPositionId())
                    .lineIndex(alDto.getLineIndex())
                    .lineStatus(LineStatus.WAITING)
                    .build();

            approval.getApprovalLineList().add(approvalLine);
        }

//        request에 approvalId 추가
        if(dto.getRequestId() != null) {
            Request request = requestRepository.findById(dto.getRequestId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 신청입니다."));

            request.updateApprovalId(approval.getId());
        }

        return approval.getId();
    }

//    결재 삭제(임시저장된 상태의 문서 삭제)
    public void discardApproval(UUID approvalId) {
        Approval approval = approvalRepository.findById(approvalId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재입니다."));
        approval.updateState(ApprovalState.DISCARDED);
    }

//    결재 승인
    public void approveApproval(UUID approvalId, UUID memberPositionId) {
        // 1. Fetch Join으로 Approval과 LineList를 한 번에 조회 (성능 최적화)
        Approval approval = approvalRepository.findByIdWithLines(approvalId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재입니다."));

        // 2. 현재 결재자의 결재 라인을 '메모리에서' 찾기
        ApprovalLine currentLine = approval.getApprovalLineList().stream()
                .filter(line -> line.getMemberPositionId().equals(memberPositionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("해당 결재 문서에 결재자로 지정되어 있지 않습니다."));

        // 3. 권한 및 순서 검증 (가장 중요!)
        if (currentLine.getLineStatus() != LineStatus.PENDING) {
            throw new BusinessException("현재 결재 순서가 아닙니다.");
        }

        // 4. 현재 결재 라인 상태 업데이트 (승인 시간 포함)
        currentLine.updateLineStatus(LineStatus.APPROVED, LocalDateTime.now());

        // 5. 다음 결재자 또는 최종 승인 처리
        int currentIndex = currentLine.getLineIndex();
        int lastIndex = approval.getApprovalLineList().size();

        if (currentIndex < lastIndex) {
            // 5-1. 다음 결재자가 있는 경우, 다음 라인의 상태를 PENDING으로 변경
            approval.getApprovalLineList().stream()
                    .filter(line -> line.getLineIndex() == currentIndex + 1)
                    .findFirst()
                    .ifPresent(nextLine -> { // 한 줄 람다를 블록 { } 으로 변경
                        // 1. 다음 라인의 상태를 PENDING으로 변경합니다.
                        nextLine.updateLineStatus(LineStatus.PENDING);

                        // 2. 다음 결재자의 memberPositionId를 변수로 받아냅니다.
                        UUID nextApproverId = nextLine.getMemberPositionId();

                        List<PositionDto> position = memberClient.getPositionList(memberPositionId, new IdListReq(List.of(nextApproverId))).getData();

                        NotificationMessage message = NotificationMessage.builder()
                                .memberId(position.get(0).getMemberId())
                                .notificationType("NT004")
                                .content("전자결재 : " + approval.getTitle() + " 문서가  도착했습니다.")
                                .targetId(approval.getId())
                                .build();

                        eventPublisher.publishEvent(message);
                    });
        } else {
            // 5-2. 현재 결재자가 마지막인 경우, 문서 전체 상태를 최종 승인으로 변경
            approval.updateState(ApprovalState.APPROVED);

            // Elasticsearch 저장을 위한 이벤트 발행
            List<PositionDto> requesterPositionInfo = memberClient.getPositionList(memberPositionId, new IdListReq(List.of(approval.getMemberPositionId()))).getData();
            if (requesterPositionInfo != null && !requesterPositionInfo.isEmpty()) {
                List<String> approverIdList = approval.getApprovalLineList().stream()
                        .map(line -> line.getMemberPositionId().toString())
                        .collect(Collectors.toList());

                ApprovalCompletedEvent approvalEvent = new ApprovalCompletedEvent(
                        approval.getId(),
                        memberPositionId,
                        approval.getTitle(),
                        requesterPositionInfo.get(0).getTitleName(),
                        requesterPositionInfo.get(0).getMemberName(),
                        approverIdList,
                        approval.getCreatedAt()
                );
                eventPublisher.publishEvent(approvalEvent);
            }

            Optional<Request> request = requestRepository.findByApprovalId(approval.getId());
            if(request.isPresent()) {
                // 근태 Request 승인 이벤트 발행 (AttendanceService에서 처리)
                AttendanceRequestApprovedEvent attendanceEvent = new AttendanceRequestApprovedEvent(
                        request.get().getId(),
                        approval.getId(),
                        ApprovalState.APPROVED,
                        LocalDateTime.now()
                );
                eventPublisher.publishEvent(attendanceEvent);
                log.error("============ [마지막 승인자] 근태 Request 승인 이벤트 발행: requestId={} ============", request.get().getId());

                // 일정 등록 이벤트 발행
                String policyTypeName = request.get().getPolicy().getPolicyTypeCode().getCodeName();
                if(policyTypeName.equals("출장")) {
                    ScheduleDto schedule = ScheduleDto.builder()
                            .originId(request.get().getId())
                            .type("CT004")
                            .title(policyTypeName)
                            .contents(request.get().getReason())
                            .startDate(request.get().getStartDateTime())
                            .endDate(request.get().getEndDateTime())
                            .memberId(request.get().getMemberId())
                            .build();
                    eventPublisher.publishEvent(schedule);
                }
                else if(policyTypeName.contains("휴가")) {
                    ScheduleDto schedule = ScheduleDto.builder()
                            .originId(request.get().getId())
                            .type("CT003")
                            .title(request.get().getRequestUnit().getCodeName())
                            .contents(request.get().getReason())
                            .startDate(request.get().getStartDateTime())
                            .endDate(request.get().getEndDateTime())
                            .memberId(request.get().getMemberId())
                            .build();
                    eventPublisher.publishEvent(schedule);
                }
            }


            UUID nextApproverId = approval.getMemberPositionId();

            List<PositionDto> position = memberClient.getPositionList(memberPositionId, new IdListReq(List.of(nextApproverId))).getData();

            NotificationMessage message = NotificationMessage.builder()
                    .memberId(position.get(0).getMemberId())
                    .notificationType("NT004")
                    .content("전자결재 : " + approval.getTitle() + " 문서가 결재가 완료되었습니다..")
                    .targetId(approval.getId())
                    .build();

            eventPublisher.publishEvent(message);
        }
    }

//    결재 반려
    public void rejectApproval(UUID approvalId, UUID memberPositionId, RejectRequestDto dto) {
        // 1. Fetch Join으로 Approval과 LineList를 한 번에 조회 (성능 최적화)
        Approval approval = approvalRepository.findByIdWithLines(approvalId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재입니다."));

        // 2. 현재 결재자의 결재 라인을 '메모리에서' 찾기
        ApprovalLine currentLine = approval.getApprovalLineList().stream()
                .filter(line -> line.getMemberPositionId().equals(memberPositionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("해당 결재 문서에 결재자로 지정되어 있지 않습니다."));

        // 3. 권한 및 순서 검증 (가장 중요!)
        if (currentLine.getLineStatus() != LineStatus.PENDING) {
            throw new BusinessException("현재 결재 순서가 아닙니다.");
        }

        // 4. 현재 결재 라인 상태 업데이트 (반려 시간 포함)
        currentLine.updateLineStatus(LineStatus.REJECTED, LocalDateTime.now());
        currentLine.reject(dto.getComment());

        // 5. 문서 전체 상태를 '반려'로 즉시 변경
        approval.updateState(ApprovalState.REJECTED);

        // 6. 근태 Request 반려 이벤트 발행 (AttendanceService에서 처리)
        Optional<Request> request = requestRepository.findByApprovalId(approval.getId());
        if(request.isPresent()) {
            AttendanceRequestApprovedEvent attendanceEvent = new AttendanceRequestApprovedEvent(
                    request.get().getId(),
                    approval.getId(),
                    ApprovalState.REJECTED,
                    LocalDateTime.now()
            );
            eventPublisher.publishEvent(attendanceEvent);
            log.info("근태 Request 반려 이벤트 발행: requestId={}", request.get().getId());
        }

        UUID nextApproverId = approval.getMemberPositionId();

        List<PositionDto> position = memberClient.getPositionList(memberPositionId, new IdListReq(List.of(nextApproverId))).getData();

        NotificationMessage message = NotificationMessage.builder()
                .memberId(position.get(0).getMemberId())
                .notificationType("NT004")
                .content("전자결재 : " + approval.getTitle() + " 문서가 결재가 완료되었습니다..")
                .targetId(approval.getId())
                .build();

        eventPublisher.publishEvent(message);
    }

//    결재 상세 조회
    @Transactional(readOnly = true)
    public ApprovalResponseDto getApproval(UUID id) {
        // 1. N+1 문제 방지를 위해 Fetch Join으로 연관 엔티티를 한 번에 조회합니다.
        Approval approval = approvalRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("결재 내역이 없습니다."));

        // 2. positionMap을 final로 선언하고, 할당은 if-else 블록 안에서 한 번만 하도록 변경
        final Map<UUID, PositionDto> positionMap;
        List<UUID> mpidList = approval.getApprovalLineList().stream()
                .map(ApprovalLine::getMemberPositionId)
                .distinct()
                .toList();

        if (!mpidList.isEmpty()) {
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(approval.getMemberPositionId(), new IdListReq(mpidList));
            if (response.isSuccess() && response.getData() != null) {
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap(); // API 호출 실패 시 빈 맵 할당
            }
        } else {
            positionMap = Collections.emptyMap(); // 결재 라인이 없을 시 빈 맵 할당
        }

        // 3. 결재 라인 DTO 리스트를 Stream으로 생성합니다. (이제 에러 없이 동작)
        List<ApprovalStepDto> lineDtoList = approval.getApprovalLineList().stream()
                .sorted(Comparator.comparing(ApprovalLine::getLineIndex)) // 순서 보장을 위해 정렬
                .map(line -> {
                    PositionDto position = positionMap.get(line.getMemberPositionId());
                    return ApprovalStepDto.builder()
                            .approverId(line.getMemberPositionId())
                            .approverName(position != null ? position.getMemberName() : null)
                            .approverPosition(position != null ? position.getTitleName() : null)
                            .approverOrganization(position != null ? position.getOrganizationName() : null)
                            .index(line.getLineIndex())
                            .status(line.getLineStatus())
                            .approveAt(line.getApprovalDate())
                            .comment(line.getComment())
                            .build();
                })
                .toList();

        // 4. 첨부파일 DTO 리스트를 Stream으로 생성합니다.
        List<AttachmentResponseDto> attachmentList = approval.getAttachmentList().stream()
                .map(attachment -> AttachmentResponseDto.builder()
                        .attachmentId(attachment.getId())
                        .attachmentUrl(attachment.getUrl())
                        .build())
                .toList();

        // 5. 최종 응답 DTO를 조립하여 반환합니다.
        return ApprovalResponseDto.builder()
                .approvalId(approval.getId())
                .title(approval.getTitle())
                .contents(approval.getContents())
                .document(DocumentResponseDto.from(approval.getApprovalDocument()))
                .lineList(lineDtoList)
                .attachmentList(attachmentList)
                .build();
    }

//    댓글 작성
    public UUID createReply(UUID approvalId, ReplyRequestDto dto, UUID memberPositionId) {
        Approval approval = approvalRepository.findById(approvalId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재입니다."));
        ApprovalReply reply = ApprovalReply.builder()
                .approval(approval)
                .contents(dto.getContents())
                .memberPositionId(memberPositionId)
                .build();
        approvalReplyRepository.save(reply);
        return reply.getId();
    }

//    댓글 조회
    @Transactional(readOnly = true)
    public Page<ReplyResponseDto> getReply(UUID approvalId, Pageable pageable) {

        // 1. (수정) Repository 메서드에 pageable 전달, Page<ApprovalReply> 반환
        Page<ApprovalReply> replyPage = approvalReplyRepository.findByApprovalId(approvalId, pageable);

        // 2. (수정) 현재 페이지의 내용(List<ApprovalReply>) 가져오기
        List<ApprovalReply> replyListOnPage = replyPage.getContent();

        // 3. (수정) 현재 페이지 댓글 작성자 정보만 가져오도록 Map 준비
        Map<UUID, PositionDto> positionMap = new HashMap<>(); // final 제거, 초기화 방식 변경
        if (!replyListOnPage.isEmpty()) { // replyList -> replyListOnPage
            // (수정) 현재 페이지의 작성자 ID 목록만 추출
            List<UUID> mpidList = replyListOnPage.stream()
                    .map(ApprovalReply::getMemberPositionId)
                    .distinct()
                    .toList();

            // (주의) memberClient.getPositionList 첫번째 파라미터 확인 필요
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(approvalId, new IdListReq(mpidList));

            if (response.isSuccess() && response.getData() != null) {
                // (수정) memberPositionId가 아니라 실제 memberId 기준으로 Map 생성
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            }
        }
        // 4. (수정) Page.map()을 사용하여 Page<ApprovalReply> -> Page<ReplyResponseDto> 변환
        final Map<UUID, PositionDto> finalPositionMap = positionMap; // 람다용 final 변수
        return replyPage.map(reply -> { // replyList.stream() 대신 replyPage.map() 사용
            PositionDto position = finalPositionMap.get(reply.getMemberPositionId());

            return ReplyResponseDto.builder()
                    .contents(reply.getContents())
                    .memberPositionId(reply.getMemberPositionId())
                    .memberName(position != null ? position.getMemberName() : null)
                    .memberPosition(position != null ? position.getTitleName() : null)
                    .memberOrganization(position != null ? position.getOrganizationName() : null)
                    .createdAt(reply.getCreatedAt())
                    .build();
        });
    }

//    결재 리스트 조회(내가 기안한 문서)
    @Transactional(readOnly = true)
    public Page<ApprovalListDto> getApprovalList(UUID memberPositionId, Pageable pageable) {

        // 1. (수정) Repository 메서드에 pageable 전달, Page<Approval> 반환
        Page<Approval> approvalPage = approvalRepository.findByMemberPositionIdAndStateWithDocument(
                memberPositionId,
                ApprovalState.PENDING,
                pageable // pageable 전달
        );

        // 2. (수정) 현재 페이지의 내용(List<Approval>) 가져오기
        List<Approval> approvalListOnPage = approvalPage.getContent();

        // 3. (수정) 현재 페이지의 기안자 정보만 가져오도록 Map 준비
        final Map<UUID, PositionDto> positionMap;
        if (!approvalListOnPage.isEmpty()) { // approvalList -> approvalListOnPage
            // (수정) 현재 페이지의 기안자 ID 목록만 추출
            List<UUID> requesterIds = approvalListOnPage.stream()
                    .map(Approval::getMemberPositionId)
                    .distinct()
                    .toList();

            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(memberPositionId, new IdListReq(requesterIds));

            if (response.isSuccess() && response.getData() != null) {
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap();
            }
        } else {
            positionMap = Collections.emptyMap();
        }

        // 4. (수정) Page.map()을 사용하여 Page<Approval> -> Page<ApprovalListDto> 변환
        return approvalPage.map(approval -> { // approvalList.stream() 대신 approvalPage.map() 사용
            PositionDto requesterPosition = positionMap.get(approval.getMemberPositionId());

            return ApprovalListDto.builder()
                    .approvalId(approval.getId())
                    .title(approval.getTitle())
                    .documentName(approval.getApprovalDocument().getDocumentName()) // Fetch Join으로 N+1 없음
                    .status(approval.getState())
                    .createAt(approval.getCreatedAt())
                    .requesterId(approval.getMemberPositionId())
                    .requesterName(requesterPosition != null ? requesterPosition.getMemberName() : null)
                    .requesterPosition(requesterPosition != null ? requesterPosition.getTitleName() : null)
                    .requesterOrganization(requesterPosition != null ? requesterPosition.getOrganizationName() : null)
                    .build();
        });
    }

//    결재 대기 문서 리스트 조회(내가 결재해야할 문서)
    @Transactional(readOnly = true)
    public Page<ApprovalListDto> getRequestedApprovalList(UUID memberPositionId, Pageable pageable) {

        // 1. (수정) Repository 메서드에 pageable 전달, Page<ApprovalLine> 반환
        Page<ApprovalLine> pendingLinesPage = approvalLineRepository.findPendingLinesWithDetails(
                memberPositionId,
                LineStatus.PENDING,
                pageable // pageable 전달
        );

        // 2. (수정) 현재 페이지의 내용(List<ApprovalLine>) 가져오기
        List<ApprovalLine> pendingLinesOnPage = pendingLinesPage.getContent();

        // 3. (수정) 현재 페이지 결재 문서의 '기안자' 정보만 가져오도록 Map 준비
        final Map<UUID, PositionDto> positionMap;
        if (!pendingLinesOnPage.isEmpty()) {
            // (수정) 현재 페이지 Line들의 부모 Approval에서 기안자 ID 목록만 추출
            List<UUID> requesterIds = pendingLinesOnPage.stream()
                    .map(line -> line.getApproval().getMemberPositionId()) // 기안자 ID 추출
                    .distinct()
                    .toList();

            // (주의) memberClient.getPositionList 첫번째 파라미터 확인 필요
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(memberPositionId, new IdListReq(requesterIds));

            if (response.isSuccess() && response.getData() != null) {
                // (수정) memberPositionId가 아니라 실제 memberId 기준으로 Map 생성
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap();
            }
        } else {
            // (수정) 조회된 결재 목록이 없으면 빈 Page 객체를 반환
            return Page.empty(pageable);
        }

        // 4. (수정) Page.map()을 사용하여 Page<ApprovalLine> -> Page<ApprovalListDto> 변환
        return pendingLinesPage.map(line -> { // pendingLines.stream() 대신 pendingLinesPage.map() 사용
            Approval approval = line.getApproval(); // JOIN FETCH로 가져온 부모 엔티티
            // Map에서 기안자 정보 조회 (기안자의 memberPositionId 사용)
            PositionDto requesterPosition = positionMap.get(approval.getMemberPositionId());

            return ApprovalListDto.builder()
                    .approvalId(approval.getId())
                    .title(approval.getTitle())
                    .documentName(approval.getApprovalDocument().getDocumentName()) // JOIN FETCH로 가져옴
                    .status(approval.getState())
                    .createAt(approval.getCreatedAt())
                    .requesterId(approval.getMemberPositionId()) // 기안자 ID
                    .requesterName(requesterPosition != null ? requesterPosition.getMemberName() : null)
                    .requesterPosition(requesterPosition != null ? requesterPosition.getTitleName() : null)
                    .requesterOrganization(requesterPosition != null ? requesterPosition.getOrganizationName() : null)
                    .build();
        });
    }

//    결재 완료 상태 문서 리스트 조회(내가 기안한 문서 중 완료 or 반려 상태인 문서들)    \
    @Transactional(readOnly = true)
    public Page<ApprovalListDto> getCompletedApprovalList(UUID memberPositionId, Pageable pageable) {
        // 1. 조회할 상태 목록을 정의합니다.
        List<ApprovalState> targetStates = List.of(ApprovalState.REJECTED, ApprovalState.APPROVED);

        // 2. (수정) Repository 메서드에 pageable 전달, Page<Approval> 반환
        Page<Approval> approvalPage = approvalRepository.findByMemberPositionIdAndStateInWithDocument(
                memberPositionId,
                targetStates,
                pageable // pageable 전달
        );

        // 3. (수정) 현재 페이지의 내용(List<Approval>) 가져오기
        List<Approval> approvalListOnPage = approvalPage.getContent();

        // 4. (수정) 현재 페이지의 기안자 정보만 가져오도록 Map 준비
        final Map<UUID, PositionDto> positionMap;
        if (!approvalListOnPage.isEmpty()) { // approvalList -> approvalListOnPage
            // (수정) 현재 페이지의 기안자 ID 목록만 추출
            List<UUID> requesterIds = approvalListOnPage.stream()
                    .map(Approval::getMemberPositionId)
                    .distinct()
                    .toList();

            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(memberPositionId, new IdListReq(requesterIds));

            if (response.isSuccess() && response.getData() != null) {
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap();
            }
        } else {
            // (수정) 조회된 결재 목록이 없으면 빈 Page 객체를 반환
            return Page.empty(pageable);
        }

        // 5. (수정) Page.map()을 사용하여 Page<Approval> -> Page<ApprovalListDto> 변환
        return approvalPage.map(approval -> { // approvalList.stream() 대신 approvalPage.map() 사용
            PositionDto requesterPosition = positionMap.get(approval.getMemberPositionId());

            return ApprovalListDto.builder()
                    .approvalId(approval.getId())
                    .title(approval.getTitle())
                    .documentName(approval.getApprovalDocument().getDocumentName()) // Fetch Join으로 N+1 없음
                    .status(approval.getState())
                    .createAt(approval.getCreatedAt())
                    .requesterId(approval.getMemberPositionId())
                    .requesterName(requesterPosition != null ? requesterPosition.getMemberName() : null)
                    .requesterPosition(requesterPosition != null ? requesterPosition.getTitleName() : null)
                    .requesterOrganization(requesterPosition != null ? requesterPosition.getOrganizationName() : null)
                    .build();
        });
    }

//    내 결재(완료)
    @Transactional(readOnly = true)
    public Page<ApprovalListDto> getCompletedApproveList(UUID memberPositionId, Pageable pageable) {
        // 1. 조회할 상태 목록을 정의합니다.
        List<ApprovalState> targetStates = List.of(ApprovalState.REJECTED, ApprovalState.APPROVED);

        // 2. (수정) Repository 메서드에 pageable 전달, Page<Approval> 반환
        //    (findByMemberPositionIdAndStateInWithDocumentAndLine -> findByLineMemberPositionIdAndStateInWithDocument 로 변경 가정)
        Page<Approval> approvalPage = approvalRepository.findByLineMemberPositionIdAndStateInWithDocument(
                memberPositionId, // 결재 라인에 포함된 사람 기준
                targetStates,
                pageable // pageable 전달
        );

        // 3. (수정) 현재 페이지의 내용(List<Approval>) 가져오기
        List<Approval> approvalListOnPage = approvalPage.getContent();

        // 4. (수정) 현재 페이지 기안자 정보만 가져오도록 Map 준비
        final Map<UUID, PositionDto> positionMap;
        if (!approvalListOnPage.isEmpty()) {
            // (수정) 현재 페이지의 기안자 ID 목록만 추출
            List<UUID> requesterIds = approvalListOnPage.stream()
                    .map(Approval::getMemberPositionId) // 기안자 ID 추출
                    .distinct()
                    .toList();

            // (주의) memberClient.getPositionList 첫번째 파라미터는 요청자의 ID여야 할 수 있음
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(memberPositionId, new IdListReq(requesterIds));

            if (response.isSuccess() && response.getData() != null) {
                // (수정) memberPositionId가 아니라 실제 memberId 기준으로 Map 생성
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap();
            }
        } else {
            // (수정) 조회된 결재 목록이 없으면 빈 Page 객체를 반환
            return Page.empty(pageable);
        }

        // 5. (수정) Page.map()을 사용하여 Page<Approval> -> Page<ApprovalListDto> 변환
        return approvalPage.map(approval -> { // approvalList.stream() 대신 approvalPage.map() 사용
            // Map에서 기안자 정보 조회 (기안자의 memberPositionId 사용)
            PositionDto requesterPosition = positionMap.get(approval.getMemberPositionId());

            return ApprovalListDto.builder()
                    .approvalId(approval.getId())
                    .title(approval.getTitle())
                    .documentName(approval.getApprovalDocument().getDocumentName()) // Fetch Join으로 N+1 없음
                    .status(approval.getState())
                    .createAt(approval.getCreatedAt())
                    .requesterId(approval.getMemberPositionId()) // 기안자 ID
                    .requesterName(requesterPosition != null ? requesterPosition.getMemberName() : null)
                    .requesterPosition(requesterPosition != null ? requesterPosition.getTitleName() : null)
                    .requesterOrganization(requesterPosition != null ? requesterPosition.getOrganizationName() : null)
                    .build();
        });
    }

//    임시 저장 상태 문서 리스트 조회
    @Transactional(readOnly = true)
    public Page<ApprovalListDto> getDraftApprovalList(UUID memberPositionId, Pageable pageable) {

        // 1. (수정) Repository 메서드에 pageable 전달, Page<Approval> 반환
        Page<Approval> approvalPage = approvalRepository.findByMemberPositionIdAndStateWithDocument(
                memberPositionId,
                ApprovalState.DRAFT, // DRAFT 상태 조회
                pageable // pageable 전달
        );

        // 2. (수정) 현재 페이지의 내용(List<Approval>) 가져오기
        List<Approval> approvalListOnPage = approvalPage.getContent();

        // 3. (수정) 현재 페이지의 기안자 정보만 가져오도록 Map 준비
        final Map<UUID, PositionDto> positionMap;
        if (!approvalListOnPage.isEmpty()) { // approvalList -> approvalListOnPage
            // (수정) 현재 페이지의 기안자 ID 목록만 추출
            List<UUID> requesterIds = approvalListOnPage.stream()
                    .map(Approval::getMemberPositionId)
                    .distinct()
                    .toList();

            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(memberPositionId, new IdListReq(requesterIds));

            if (response.isSuccess() && response.getData() != null) {
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap();
            }
        } else {
            // (수정) 조회된 결재 목록이 없으면 빈 Page 객체를 반환
            return Page.empty(pageable);
        }

        // 4. (수정) Page.map()을 사용하여 Page<Approval> -> Page<ApprovalListDto> 변환
        return approvalPage.map(approval -> { // approvalList.stream() 대신 approvalPage.map() 사용
            PositionDto requesterPosition = positionMap.get(approval.getMemberPositionId());

            return ApprovalListDto.builder()
                    .approvalId(approval.getId())
                    .title(approval.getTitle())
                    .documentName(approval.getApprovalDocument().getDocumentName()) // Fetch Join으로 N+1 없음
                    .status(approval.getState())
                    .createAt(approval.getCreatedAt())
                    .requesterId(approval.getMemberPositionId())
                    .requesterName(requesterPosition != null ? requesterPosition.getMemberName() : null)
                    .requesterPosition(requesterPosition != null ? requesterPosition.getTitleName() : null)
                    .requesterOrganization(requesterPosition != null ? requesterPosition.getOrganizationName() : null)
                    .build();
        });
    }

//    첨부파일 등록 및 수정
    public void patchFile(UUID id, AttachmentRequestDto dto , List<MultipartFile> newFiles) {
        Approval approval = approvalRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재입니다."));

        processDeletions(approval, dto.getExistingFileIds());

        processAdditions(approval, newFiles);
    }

    private void processDeletions(Approval approval, List<UUID> idsToKeep) {
        List<Attachment> currentFiles = approval.getAttachmentList();

        List<Attachment> filesToDelete = currentFiles.stream()
                .filter(file -> !idsToKeep.contains(file.getId()))
                .toList();

        if(!filesToDelete.isEmpty()) {
            filesToDelete.forEach(file -> s3Uploader.delete(file.getUrl()));

            currentFiles.removeAll(filesToDelete);
        }
    }

    private void processAdditions(Approval approval, List<MultipartFile> newFiles) {
        if(newFiles != null && !newFiles.isEmpty()) {
            newFiles.forEach(file -> {
                String uploadUrl = s3Uploader.upload(file, "attachment");
                Attachment newAttachment = Attachment.builder()
                        .url(uploadUrl)
                        .approval(approval)
                        .build();

                approval.getAttachmentList().add(newAttachment);
            });
        }
    }

//    결재 상태 변경용
    public UUID updateState(UUID id, ApprovalState state) {
        Approval approval = approvalRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(""));
        approval.updateState(state);
        return approval.getId();
    }

//    상단 통계용
    @Transactional(readOnly = true)
    public StatsResponseDto getStats(UUID memberPositionId) {
        int pending = approvalLineRepository.findByMemberPositionIdAndLineStatus(
                memberPositionId,
                LineStatus.PENDING
        ).stream().map(ApprovalLine::getApproval).toList().size();


//        내 기안(진행중)
        int request = approvalRepository.countByMemberPositionIdAndState(memberPositionId, ApprovalState.PENDING);
//        int request = approvalRepository.countByState(ApprovalState.PENDING);

//        내 기안(완료)
        List<ApprovalState> stateList = new ArrayList<>();
        stateList.add(ApprovalState.REJECTED);
        stateList.add(ApprovalState.APPROVED);
        int complete = approvalRepository.countByMemberPositionIdAndStateIn(memberPositionId, stateList);
//        int complete = approvalRepository.countByStateIn(stateList);

//        내 결재(완료)
        int approveComplete = approvalRepository.countByLineMemberPositionIdAndStateIn(memberPositionId, stateList);

//        임시 저정함
        int draft = approvalRepository.countByMemberPositionIdAndState(memberPositionId, ApprovalState.DRAFT);
//        int draft = approvalRepository.countByState(ApprovalState.DRAFT);

        return StatsResponseDto.builder()
                .pendingCount(pending)
                .requestCount(request)
                .completeCount(complete)
                .approveCompleteCount(approveComplete)
                .draftCount(draft)
                .build();
    }

//    문서 결재정책 생성 및 수정
    public void setPolicies(UUID documentID, List<DocumentPolicyDto> dtoList) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentID)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문서입니다."));
        document.getPolicyList().clear();

        if (dtoList != null && !dtoList.isEmpty()) {
            List<ApprovalPolicy> newPolicies = dtoList.stream()
                    .map(dto -> ApprovalPolicy.builder()
                            .approvalDocument(document) // 부모-자식 관계 설정
//                            .companyId(dto.getCompanyId())
                            .requirementType(dto.getRequirementType())
                            .requirementId(dto.getRequirementId())
                            .lineIndex(dto.getLineIndex())
                            .build())
                    .toList();

            document.getPolicyList().addAll(newPolicies);
        }
    }

//    문서 정책 조회
    public List<PolicyResDto> getPolicies(UUID documentId, UUID memberId, UUID memberPositionId) {
        List<ApprovalPolicy> policies = approvalPolicyRepository.findByApprovalDocument_Id(documentId);

        IdListReq idListReq = new IdListReq(policies.stream().filter(a->a.getRequirementType()
                .equals(RequirementType.MEMBER_POSITION)).map(ApprovalPolicy::getRequirementId).toList());

        List<PositionDto> position = memberClient.getPositionList(memberPositionId, idListReq).getData();
        Map<UUID, PositionDto> positionMap = new HashMap<>();
        if(!position.isEmpty()) {
            positionMap = position.stream().collect(Collectors.toMap(PositionDto::getMemberPositionId, pos -> pos));
        }
        else {
            positionMap = Collections.emptyMap();
        }

        List<TitleRes> titles = memberClient.getTitle(memberId, memberPositionId).getData();
        Map<UUID, TitleRes> titleMap = new HashMap<>();
        if(!titles.isEmpty()) {
            titleMap = titles.stream().collect(Collectors.toMap(TitleRes::getId, pos -> pos));
        }
        else {
            titleMap = Collections.emptyMap();
        }


        List<PolicyResDto> dtoList = new ArrayList<>();
        for(ApprovalPolicy p : policies) {
            String name;
            if(p.getRequirementType().equals(RequirementType.MEMBER_POSITION)) {
                name = positionMap.get(p.getRequirementId()).getMemberName();
                name += "(" + positionMap.get(p.getRequirementId()).getTitleName() + ")";
            }
            else {
                name = titleMap.get(p.getRequirementId()).getName();
            }
            PolicyResDto dto = PolicyResDto.builder()
                    .requirementType(p.getRequirementType())
                    .requirementId(p.getRequirementId())
                    .name(name)
                    .lineIndex(p.getLineIndex())
                    .build();
            dtoList.add(dto);
        }

        return dtoList;
    }

    // 결재 데이터 변경이 완료된 후 Elasticsearch 동기화를 위한 이벤트 저장
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handleApprovalCompletedEvent(ApprovalCompletedEvent event) {
        saveSearchOutboxEvent(event);
    }

    // elastic search에 저장
    public void saveSearchOutboxEvent(ApprovalCompletedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            ApprovalSearchOutboxEvent outboxEvent = ApprovalSearchOutboxEvent.builder()
                    .topic("approval-completed-events")
                    .aggregateId(event.getApprovalId())
                    .payload(payload)
                    .build();
            approvalSearchOutboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new SerializationException("데이터 직렬화 실패");
        }
    }
}
