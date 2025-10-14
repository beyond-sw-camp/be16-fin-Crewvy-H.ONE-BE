package com.crewvy.workforce_service.approval.service;

import com.crewvy.common.S3.S3Uploader;
import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.constant.LineStatus;
import com.crewvy.workforce_service.approval.dto.request.*;
import com.crewvy.workforce_service.approval.dto.response.*;
import com.crewvy.workforce_service.approval.entity.*;
import com.crewvy.workforce_service.approval.repository.ApprovalDocumentRepository;
import com.crewvy.workforce_service.approval.repository.ApprovalLineRepository;
import com.crewvy.workforce_service.approval.repository.ApprovalReplyRepository;
import com.crewvy.workforce_service.approval.repository.ApprovalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalService {
    private final ApprovalRepository approvalRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final ApprovalReplyRepository approvalReplyRepository;
    private final S3Uploader s3Uploader;

//    문서 양식 생성
    public UUID uploadDocument(UploadDocumentDto dto) {
        ApprovalDocument newDocument = ApprovalDocument.builder()
                .documentName(dto.getDocumentName())
                .metadata(dto.getMetadata())
                .build();
        approvalDocumentRepository.save(newDocument);

        return newDocument.getId();
    }

//    문서 양식 관련 수정(정책 추가)
    public UUID updateDocument(UpdateDocumentDto dto) {
        ApprovalDocument document = approvalDocumentRepository.findById(dto.getDocumentId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문서입니다."));
        document.updateDocument(dto);

        document.getApprovalPolicy().clear();

        for(DocumentPolicyDto dp : dto.getPolicyDtoList()) {
            ApprovalPolicy approvalPolicy = ApprovalPolicy
                    .builder()
                    .roleId(dp.getRoleId())
                    .memberPositionId(dp.getMemberPositionId())
                    .lineIndex(dp.getLineIndex())
                    .build();
            document.addApprovalPolicy(approvalPolicy);
        }

        return document.getId();
    }

//    문서 양식 조회
    public DocumentResponseDto getDocument(UUID id) {
        ApprovalDocument document = approvalDocumentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문서입니다."));
        return DocumentResponseDto.builder()
                .documentId(document.getId())
                .documentName(document.getDocumentName())
                .metadata(document.getMetadata())
                .build();
    }

//    문서 양식 리스트 조회
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

        // 3. 결재 라인(자식) 생성
        for (ApprovalLineRequestDto alDto : dto.getLineDtoList()) {
            LineStatus currentStatus;

            if (alDto.getLineIndex() == 1) {
                currentStatus = LineStatus.APPROVED;
            } else if (alDto.getLineIndex() == 2) {
                currentStatus = LineStatus.PENDING;
            } else {
                currentStatus = LineStatus.WAITING;
            }

            ApprovalLine approvalLine = ApprovalLine.builder()
                    .approval(approval)
                    .memberPositionId(alDto.getMemberPositionId())
                    .lineIndex(alDto.getLineIndex())
                    .lineStatus(currentStatus)
                    .build();

            approval.getApprovalLineList().add(approvalLine);
        }

        // 4. 최종 상태 결정: 결재 라인이 1명뿐인 경우 최종 승인 처리
        if (dto.getLineDtoList().size() == 1) {
            approval.updateState(ApprovalState.APPROVED);
        }

        // 5. 부모 엔티티를 한 번만 저장
        approvalRepository.save(approval);

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
        Approval approval = approvalRepository.findById(approvalId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재입니다."));
        ApprovalLine approvalLine = approvalLineRepository.findByApprovalAndMemberPositionId(approval, memberPositionId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재자입니다."));
        approvalLine.updateLineStatus(LineStatus.APPROVED);

        ApprovalLine lastIndex = approvalLineRepository.findFirstByApprovalOrderByLineIndexDesc(approval).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재라인입니다."));
        if(approvalLine.getLineIndex() == lastIndex.getLineIndex()) {
            approval.updateState(ApprovalState.APPROVED);
        }
    }

//    결재 반려
    public void rejectApproval(UUID approvalId, UUID memberId) {
        Approval approval = approvalRepository.findById(approvalId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재입니다."));
        ApprovalLine approvalLine = approvalLineRepository.findByApprovalAndMemberPositionId(approval, memberId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재자입니다."));
        approvalLine.updateLineStatus(LineStatus.REJECTED);
        approval.updateState(ApprovalState.REJECTED);
    }

//    결재 상세 조회
    public ApprovalResponseDto getApproval(UUID id) {
        Approval approval = approvalRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("결재 내역이 없습니다."));

//        결재 라인
        List<ApprovalStepDto> lineList = new ArrayList<>();
        for(ApprovalLine a : approval.getApprovalLineList()) {
            ApprovalStepDto dto = ApprovalStepDto.builder()
                    .approverId(a.getId())
//                    .approverName()
//                    .approverGrade()
                    .index(a.getLineIndex())
                    .status(a.getLineStatus())
                    .build();
            lineList.add(dto);
        }

//        결재 첨부파일
        List<AttachmentResponseDto> attachmentList = new ArrayList<>();
        for(Attachment a : approval.getAttachmentList()) {
            AttachmentResponseDto dto = AttachmentResponseDto.builder()
                    .attachmentId(a.getId())
                    .attachmentUrl(a.getUrl())
                    .build();
            attachmentList.add(dto);
        }

        return ApprovalResponseDto.builder()
                .approvalId(approval.getId())
                .title(approval.getTitle())
                .contents(approval.getContents())
                .document(DocumentResponseDto.builder()
                        .documentName(approval.getApprovalDocument().getDocumentName())
                        .metadata(approval.getApprovalDocument().getMetadata())
                        .build())
                .lineList(lineList)
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
    public List<ReplyResponseDto> getReply(UUID approvalId) {
        List<ApprovalReply> replyList = approvalReplyRepository.findByApprovalId(approvalId);
        List<ReplyResponseDto> dtoList = new ArrayList<>();
        for(ApprovalReply re : replyList) {
            ReplyResponseDto dto = ReplyResponseDto.builder()
                    .contents(re.getContents())
                    .memberPositionId(re.getMemberPositionId())
//                    .memberName()
//                    .memberGrade()
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

//    결재 리스트 조회(내가 기안한 문서)
    public List<ApprovalListDto> getApprovalList(UUID memberPositionId) {
        List<Approval> approvalList = approvalRepository.findByMemberPositionIdAndState(memberPositionId, ApprovalState.PENDING);
//        List<Approval> approvalList = approvalRepository.findByState(ApprovalState.PENDING);
//        List<Approval> approvalList = approvalRepository.findAll();
        List<ApprovalListDto> dtoList  = new ArrayList<>();
        for(Approval a : approvalList) {
            ApprovalListDto dto = ApprovalListDto.builder()
                    .approvalId(a.getId())
                    .title(a.getTitle())
                    .documentName(a.getApprovalDocument().getDocumentName())
                    .requesterId(a.getMemberPositionId()) // 기안자 ID -> 기안자명, 소속, 직급으로 대체 예정
                    .status(a.getState())
                    .createAt(a.getCreatedAt())
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

//    결재 대기 문서 리스트 조회(내가 결재해야할 문서)
    public List<ApprovalListDto> getRequsetedApprovalList(UUID memberPositionId) {
        // 특정 사용자의 '대기(PENDING)' 상태인 결재 라인을 모두 찾기
        List<ApprovalLine> pendingLines = approvalLineRepository.findByMemberPositionIdAndLineStatus(
                memberPositionId,
                LineStatus.PENDING // '대기' 상태를 나타내는 ENUM
        );

        List<ApprovalListDto> listDto = new ArrayList<>();
        for(ApprovalLine a : pendingLines) {
            ApprovalListDto approvalListDto = ApprovalListDto.builder()
                    .approvalId(a.getApproval().getId())
                    .title(a.getApproval().getTitle())
                    .documentName(a.getApproval().getApprovalDocument().getDocumentName())
                    .requesterId(a.getApproval().getMemberPositionId()) // 기안자 ID -> 기안자명, 소속, 직급으로 대체 예정
                    .status(a.getApproval().getState())
                    .createAt(a.getApproval().getCreatedAt())
                    .build();
            listDto.add(approvalListDto);
        }

        return listDto;
    }

//    결재 완료 상태 문서 리스트 조회(내가 기안한 문서 중 완료 or 반려 상태인 문서들)
    public List<ApprovalListDto> getCompletedApprovalList(UUID memberPositionId) {
        List<ApprovalState> stateList = new ArrayList<>();
        stateList.add(ApprovalState.REJECTED);
        stateList.add(ApprovalState.APPROVED);
        List<Approval> approvalList = approvalRepository.findByMemberPositionIdAndStateIn(memberPositionId, stateList);
//        List<Approval> approvalList = approvalRepository.findByStateIn(stateList);

        List<ApprovalListDto> dtoList = new ArrayList<>();
        for(Approval a : approvalList) {
            ApprovalListDto dto = ApprovalListDto.builder()
                    .approvalId(a.getId())
                    .title(a.getTitle())
                    .documentName(a.getApprovalDocument().getDocumentName())
                    .requesterId(a.getMemberPositionId()) // 기안자 ID -> 기안자명, 소속, 직급으로 대체 예정
                    .status(a.getState())
                    .createAt(a.getCreatedAt())
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

//    임시 저장 상태 문서 리스트 조회
    public List<ApprovalListDto> getDraftApprovalList(UUID memberPositionId) {
        List<Approval> approvalList = approvalRepository.findByMemberPositionIdAndState(memberPositionId, ApprovalState.DRAFT);
//        List<Approval> approvalList = approvalRepository.findByState(ApprovalState.DRAFT);
        List<ApprovalListDto> dtoList = new ArrayList<>();
        for(Approval a : approvalList) {
            ApprovalListDto dto = ApprovalListDto.builder()
                    .approvalId(a.getId())
                    .title(a.getTitle())
                    .documentName(a.getApprovalDocument().getDocumentName())
                    .requesterId(a.getMemberPositionId()) // 기안자 ID -> 기안자명, 소속, 직급으로 대체 예정
                    .status(a.getState())
                    .createAt(a.getCreatedAt())
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
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
}
