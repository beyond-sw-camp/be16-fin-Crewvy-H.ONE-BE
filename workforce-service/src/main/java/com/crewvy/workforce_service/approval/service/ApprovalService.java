package com.crewvy.workforce_service.approval.service;

import com.crewvy.common.S3.S3Uploader;
import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.notification.NotificationRedis;
import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.constant.LineStatus;
import com.crewvy.workforce_service.approval.constant.RequirementType;
import com.crewvy.workforce_service.approval.dto.request.*;
import com.crewvy.workforce_service.approval.dto.response.*;
import com.crewvy.workforce_service.approval.entity.*;
import com.crewvy.workforce_service.approval.repository.ApprovalDocumentRepository;
import com.crewvy.workforce_service.approval.repository.ApprovalLineRepository;
import com.crewvy.workforce_service.approval.repository.ApprovalReplyRepository;
import com.crewvy.workforce_service.approval.repository.ApprovalRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.MemberDto;
import com.crewvy.workforce_service.feignClient.dto.response.OrganizationNodeDto;
import com.crewvy.workforce_service.feignClient.dto.response.PositionDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalService {
    private final ApprovalRepository approvalRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final ApprovalReplyRepository approvalReplyRepository;
    private final S3Uploader s3Uploader;
    private final MemberClient memberClient;
    private final NotificationRedis notification;

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
    public DocumentResponseDto getDocument(UUID id, UUID memberPositionId, UUID memberId) {
        // 1. 문서와 정책 목록을 한 번에 조회합니다 (N+1 방지).
        ApprovalDocument document = approvalDocumentRepository.findByIdWithPolicies(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문서입니다."));

        // 2. 조직도는 한 번만 조회하여 재사용합니다.
        List<OrganizationNodeDto> orgTree = memberClient.getOrganization(memberId).getData();

        // 3. 각 정책을 해석하여 '순서(lineIndex)'와 '찾아야 할 결재자 ID'를 Pair로 묶어 저장합니다.
        List<Pair<Integer, UUID>> resolvedPolicies = new ArrayList<>();
        for (ApprovalPolicy policy : document.getPolicyList()) {
            UUID approverId;
            if (policy.getRequirementType() == RequirementType.TITLE) {
                approverId = findApproverByTitle(orgTree, memberPositionId, policy.getRequirementId());
            } else { // MEMBER_POSITION 또는 ROLE
                approverId = policy.getRequirementId();
            }

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

        // 6. 완성된 추천 결재자 목록을 최종 DTO에 담아 반환합니다.
        return DocumentResponseDto.builder()
                .documentId(document.getId())
                .documentName(document.getDocumentName())
                .metadata(document.getMetadata())
                .policy(policyLine)
                .build();
    }

    private UUID findApproverByTitle(List<OrganizationNodeDto> orgTree, UUID myMemberPositionId, UUID requiredTitleId) {
        // 1. 먼저 조직도 전체에서 '나'의 위치와 경로를 찾습니다.
        List<OrganizationNodeDto> pathToMe = findPathToMember(orgTree, myMemberPositionId);

        if (pathToMe == null || pathToMe.isEmpty()) {
            throw new EntityNotFoundException("요청자를 조직도에서 찾을 수 없습니다.");
        }

        // 2. '나'와 가장 가까운 조직(팀)부터 상위 조직으로 거슬러 올라가며 탐색합니다.
        for (int i = pathToMe.size() - 1; i >= 0; i--) {
            OrganizationNodeDto currentOrg = pathToMe.get(i);

            // 3. 현재 조직의 멤버들 중에서 필요한 직책(titleId)을 가진 사람을 찾습니다.
            Optional<MemberDto> foundApprover = currentOrg.getMembers().stream() // .members() -> .getMembers()
                    .filter(member -> requiredTitleId.equals(member.getTitleId())) // .titleId() -> .getTitleId()
                    .findFirst();

            if (foundApprover.isPresent()) {
                // 4. 찾았으면, 그 사람의 memberPositionId를 즉시 반환하고 종료합니다.
                return foundApprover.get().getMemberPositionId(); // .memberPositionId() -> .getMemberPositionId()
            }
        }

        // 5. 최상위 조직까지 올라갔는데도 못 찾은 경우
        throw new BusinessException("해당 직책을 가진 상위 결재자를 찾을 수 없습니다.");
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

//    문서 양식 리스트 조회
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
        approvalRepository.save(approval);

//        알림 전송
        if(alarmId != null) {
            NotificationMessage message = NotificationMessage.builder()
                    .memberId(alarmId)
                    .notificationType("APPROVAL")
                    .content("전자결재 : " + approval.getTitle() + " 문서가  도착했습니다.")
                    .build();

            try {
                notification.sendNotification(message);
            }
            catch (Exception e) {
                throw new BusinessException("레디스 알림 전송 실패");
            }

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
                    .ifPresent(nextLine -> { // 👈 한 줄 람다를 블록 { } 으로 변경
                        // 1. 다음 라인의 상태를 PENDING으로 변경합니다.
                        nextLine.updateLineStatus(LineStatus.PENDING);

                        // 2. ⭐ 다음 결재자의 memberPositionId를 변수로 받아냅니다.
                        UUID nextApproverId = nextLine.getMemberPositionId();

                        List<PositionDto> position = memberClient.getPositionList(memberPositionId, new IdListReq(List.of(nextApproverId))).getData();

                        NotificationMessage message = NotificationMessage.builder()
                                .memberId(position.get(0).getMemberId())
                                .notificationType("approval")
                                .content("전자결재 : " + approval.getTitle() + " 문서가  도착했습니다.")
                                .build();

                        try {
                            notification.sendNotification(message);
                        }
                        catch (Exception e) {
                            throw new BusinessException("레디스 알림 전송 실패");
                        }
                    });
        } else {
            // 5-2. 현재 결재자가 마지막인 경우, 문서 전체 상태를 최종 승인으로 변경
            approval.updateState(ApprovalState.APPROVED);

            UUID nextApproverId = approval.getMemberPositionId();

            List<PositionDto> position = memberClient.getPositionList(memberPositionId, new IdListReq(List.of(nextApproverId))).getData();

            NotificationMessage message = NotificationMessage.builder()
                    .memberId(position.get(0).getMemberId())
                    .notificationType("APPROVAL")
                    .content("전자결재 : " + approval.getTitle() + " 문서가 결재가 완료되었습니다..")
                    .build();

            try {
                notification.sendNotification(message);
            }
            catch (Exception e) {
                throw new BusinessException("레디스 알림 전송 실패");
            }
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

        UUID nextApproverId = approval.getMemberPositionId();

        List<PositionDto> position = memberClient.getPositionList(memberPositionId, new IdListReq(List.of(nextApproverId))).getData();

        NotificationMessage message = NotificationMessage.builder()
                .memberId(position.get(0).getMemberId())
                .notificationType("APPROVAL")
                .content("전자결재 : " + approval.getTitle() + " 문서가 결재가 완료되었습니다..")
                .build();

        try {
            notification.sendNotification(message);
        }
        catch (Exception e) {
            throw new BusinessException("레디스 알림 전송 실패");
        }
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
    public List<ReplyResponseDto> getReply(UUID approvalId) {
        // 1. 특정 결재 문서에 달린 댓글(Reply) 목록을 모두 조회합니다.
        List<ApprovalReply> replyList = approvalReplyRepository.findByApprovalId(approvalId);

        // 2. 댓글 작성자들의 Position 정보를 조회하기 위해 Map을 준비합니다.
        Map<UUID, PositionDto> positionMap = new HashMap<>();
        if (!replyList.isEmpty()) {
            // 댓글 목록에서 작성자(memberPositionId) ID들을 추출합니다.
            List<UUID> mpidList = replyList.stream()
                    .map(ApprovalReply::getMemberPositionId)
                    .distinct()
                    .toList();

            // FeignClient를 호출합니다.
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(approvalId, new IdListReq(mpidList));

            if (response.isSuccess() && response.getData() != null) {
                // 3. 빠른 조회를 위해 PositionDto 리스트를 Map으로 변환합니다.
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            }
        }

        // 4. Stream을 사용하여 Reply 엔티티를 ReplyResponseDto로 변환합니다.
        final Map<UUID, PositionDto> finalPositionMap = positionMap; // 람다에서 사용하기 위해 final 변수로 복사
        return replyList.stream().map(reply -> {
            // Map에서 현재 댓글의 작성자 ID와 일치하는 PositionDto를 찾습니다.
            PositionDto position = finalPositionMap.get(reply.getMemberPositionId());

            // ReplyResponseDto를 만들 때, 찾은 PositionDto의 데이터를 함께 넣어줍니다.
            return ReplyResponseDto.builder()
                    .contents(reply.getContents())
                    .memberPositionId(reply.getMemberPositionId())
                    .memberName(position != null ? position.getMemberName() : null)
                    .memberPosition(position != null ? position.getTitleName() : null)
                    .memberOrganization(position != null ? position.getOrganizationName() : null)
                    .createdAt(reply.getCreatedAt()) // 생성일자도 추가하면 좋습니다.
                    .build();
        }).toList();
    }

//    결재 리스트 조회(내가 기안한 문서)
    @Transactional(readOnly = true)
    public List<ApprovalListDto> getApprovalList(UUID memberPositionId) {
        // 1. N+1 문제가 해결된 쿼리로 결재 목록 조회
        List<Approval> approvalList = approvalRepository.findByMemberPositionIdAndStateWithDocument(memberPositionId, ApprovalState.PENDING);

        // 2. 기안자 정보를 가져오기 위해 Map 준비
        final Map<UUID, PositionDto> positionMap;
        if (!approvalList.isEmpty()) {
            // 기안자들의 ID 목록 추출
            List<UUID> requesterIds = approvalList.stream()
                    .map(Approval::getMemberPositionId)
                    .distinct()
                    .toList();

            // FeignClient로 한 번에 정보 조회
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

        // 3. Stream API를 사용하여 최종 DTO 리스트 생성
        return approvalList.stream()
                .map(approval -> {
                    // Map에서 현재 결재 문서의 기안자 정보를 빠르게 조회
                    PositionDto requesterPosition = positionMap.get(approval.getMemberPositionId());

                    return ApprovalListDto.builder()
                            .approvalId(approval.getId())
                            .title(approval.getTitle())
                            .documentName(approval.getApprovalDocument().getDocumentName())
                            .status(approval.getState())
                            .createAt(approval.getCreatedAt())
                            // ID 대신 조회해온 이름, 직책, 부서 정보로 대체
                            .requesterId(approval.getMemberPositionId())
                            .requesterName(requesterPosition != null ? requesterPosition.getMemberName() : null)
                            .requesterPosition(requesterPosition != null ? requesterPosition.getTitleName() : null)
                            .requesterOrganization(requesterPosition != null ? requesterPosition.getOrganizationName() : null)
                            .build();
                })
                .toList();
    }

//    결재 대기 문서 리스트 조회(내가 결재해야할 문서)
    @Transactional(readOnly = true)
    public List<ApprovalListDto> getRequsetedApprovalList(UUID memberPositionId) {
        // 1. 최적화된 쿼리로 '내가 결재할' 라인 목록을 조회 (Approval, Document 정보 포함)
        List<ApprovalLine> pendingLines = approvalLineRepository.findPendingLinesWithDetails(
                memberPositionId,
                LineStatus.PENDING
        );

        // 2. 각 결재 문서의 '기안자' 정보를 가져오기 위해 Map 준비
        final Map<UUID, PositionDto> positionMap;
        if (!pendingLines.isEmpty()) {
            // 부모 Approval 객체에서 기안자(requester) ID 목록 추출
            List<UUID> requesterIds = pendingLines.stream()
                    .map(line -> line.getApproval().getMemberPositionId())
                    .distinct()
                    .toList();

            // FeignClient로 한 번에 정보 조회
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(memberPositionId, new IdListReq(requesterIds));

            if (response.isSuccess() && response.getData() != null) {
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap();
            }
        } else {
            return Collections.emptyList();
        }

        // 3. Stream API를 사용하여 최종 DTO 리스트 생성
        return pendingLines.stream()
                .map(line -> {
                    Approval approval = line.getApproval(); // JOIN FETCH로 가져온 부모 엔티티
                    PositionDto requesterPosition = positionMap.get(approval.getMemberPositionId());

                    return ApprovalListDto.builder()
                            .approvalId(approval.getId())
                            .title(approval.getTitle())
                            .documentName(approval.getApprovalDocument().getDocumentName())
                            .status(approval.getState())
                            .createAt(approval.getCreatedAt())
                            .requesterId(approval.getMemberPositionId())
                            .requesterName(requesterPosition != null ? requesterPosition.getMemberName() : null)
                            .requesterPosition(requesterPosition != null ? requesterPosition.getTitleName() : null)
                            .requesterOrganization(requesterPosition != null ? requesterPosition.getOrganizationName() : null)
                            .build();
                })
                .toList();
    }

//    결재 완료 상태 문서 리스트 조회(내가 기안한 문서 중 완료 or 반려 상태인 문서들)    \
    @Transactional(readOnly = true)
    public List<ApprovalListDto> getCompletedApprovalList(UUID memberPositionId) {
        // 1. 조회할 상태 목록을 정의합니다.
        List<ApprovalState> targetStates = List.of(ApprovalState.REJECTED, ApprovalState.APPROVED);

        // 2. 최적화된 쿼리로 '완료' 또는 '반려' 상태의 결재 목록 조회
        List<Approval> approvalList = approvalRepository.findByMemberPositionIdAndStateInWithDocument(memberPositionId, targetStates);

        // 3. 기안자 정보를 가져오기 위해 Map 준비
        final Map<UUID, PositionDto> positionMap;
        if (!approvalList.isEmpty()) {
            List<UUID> requesterIds = approvalList.stream()
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
            return Collections.emptyList();
        }

        // 4. Stream API를 사용하여 최종 DTO 리스트 생성
        return approvalList.stream()
                .map(approval -> {
                    PositionDto requesterPosition = positionMap.get(approval.getMemberPositionId());

                    return ApprovalListDto.builder()
                            .approvalId(approval.getId())
                            .title(approval.getTitle())
                            .documentName(approval.getApprovalDocument().getDocumentName())
                            .status(approval.getState())
                            .createAt(approval.getCreatedAt())
                            .requesterId(approval.getMemberPositionId())
                            .requesterName(requesterPosition != null ? requesterPosition.getMemberName() : null)
                            .requesterPosition(requesterPosition != null ? requesterPosition.getTitleName() : null)
                            .requesterOrganization(requesterPosition != null ? requesterPosition.getOrganizationName() : null)
                            .build();
                })
                .toList();
    }

//    임시 저장 상태 문서 리스트 조회
    @Transactional(readOnly = true)
    public List<ApprovalListDto> getDraftApprovalList(UUID memberPositionId) {
        // 1. 최적화된 쿼리로 '임시저장(DRAFT)' 상태의 결재 목록 조회
        List<Approval> approvalList = approvalRepository.findByMemberPositionIdAndStateWithDocument(memberPositionId, ApprovalState.DRAFT);

        // 2. 기안자 정보를 가져오기 위해 Map 준비
        final Map<UUID, PositionDto> positionMap;
        if (!approvalList.isEmpty()) {
            // 기안자들의 ID 목록 추출
            List<UUID> requesterIds = approvalList.stream()
                    .map(Approval::getMemberPositionId)
                    .distinct()
                    .toList();

            // FeignClient로 한 번에 정보 조회
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(memberPositionId, new IdListReq(requesterIds));

            if (response.isSuccess() && response.getData() != null) {
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap();
            }
        } else {
            // 조회된 결재 목록이 없으면 바로 빈 리스트를 반환
            return Collections.emptyList();
        }

        // 3. Stream API를 사용하여 최종 DTO 리스트 생성
        return approvalList.stream()
                .map(approval -> {
                    // Map에서 현재 결재 문서의 기안자 정보를 빠르게 조회
                    PositionDto requesterPosition = positionMap.get(approval.getMemberPositionId());

                    return ApprovalListDto.builder()
                            .approvalId(approval.getId())
                            .title(approval.getTitle())
                            .documentName(approval.getApprovalDocument().getDocumentName())
                            .status(approval.getState())
                            .createAt(approval.getCreatedAt())
                            .requesterId(approval.getMemberPositionId())
                            .requesterName(requesterPosition != null ? requesterPosition.getMemberName() : null)
                            .requesterPosition(requesterPosition != null ? requesterPosition.getTitleName() : null)
                            .requesterOrganization(requesterPosition != null ? requesterPosition.getOrganizationName() : null)
                            .build();
                })
                .toList();
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


//        진행중인 결재(내가 기안한)
        int request = approvalRepository.countByMemberPositionIdAndState(memberPositionId, ApprovalState.PENDING);
//        int request = approvalRepository.countByState(ApprovalState.PENDING);

//        완료된 결재
        List<ApprovalState> stateList = new ArrayList<>();
        stateList.add(ApprovalState.REJECTED);
        stateList.add(ApprovalState.APPROVED);
        int complete = approvalRepository.countByMemberPositionIdAndStateIn(memberPositionId, stateList);
//        int complete = approvalRepository.countByStateIn(stateList);

        int draft = approvalRepository.countByMemberPositionIdAndState(memberPositionId, ApprovalState.DRAFT);
//        int draft = approvalRepository.countByState(ApprovalState.DRAFT);

        return StatsResponseDto.builder()
                .pendingCount(pending)
                .requestCount(request)
                .completeCount(complete)
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
}
