package com.crewvy.workforce_service.approval.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.approval.dto.request.*;
import com.crewvy.workforce_service.approval.dto.response.*;
import com.crewvy.workforce_service.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/approval")
public class ApprovalController {
    private final ApprovalService approvalService;


    @GetMapping("/get-document/{id}")
    public ResponseEntity<?> getDocument(@PathVariable UUID id,
                                         @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @RequestHeader("X-User-UUID") UUID memberId
    ) {
        DocumentResponseDto document = approvalService.getDocument(id, memberPositionId, memberId);
        return new ResponseEntity<>(
                ApiResponse.success(document, "양식 상세 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/get-document-list")
    public ResponseEntity<?> getDocumentList() {
        List<DocumentResponseDto> documents = approvalService.getDocumentList();
        return new ResponseEntity<>(
                ApiResponse.success(documents, "양식 리스트 조회"),
                HttpStatus.OK
        );
    }

    @PostMapping("/create-approval")
    public ResponseEntity<?> createApproval(@RequestBody CreateApprovalDto dto,
                                            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId
    ) {
        UUID approvalId = approvalService.createApproval(dto, memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(approvalId, "결재 생성"),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/draft-approval")
    public ResponseEntity<?> draftApproval(@RequestBody CreateApprovalDto dto,
                                           @RequestHeader("X-User-MemberPositionId") UUID memberPositionId
    ) {
        UUID approvalId = approvalService.draftApproval(dto, memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(approvalId, "임시저장"),
                HttpStatus.CREATED
        );
    }

    @DeleteMapping("/discard-approval/{id}")
    public ResponseEntity<?> discardApproval(@PathVariable UUID id) {
        approvalService.discardApproval(id);
        return new ResponseEntity<>(
                ApiResponse.success(id, "문서 삭제"),
                HttpStatus.OK
        );
    }

    @PatchMapping("/approve/{approvalId}")
        public ResponseEntity<?> approveApproval(@PathVariable UUID approvalId,
                                                 @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        approvalService.approveApproval(approvalId, memberPositionId);
        return new ResponseEntity<>(ApiResponse.success(approvalId, "승인"), HttpStatus.OK);
    }

    @PatchMapping("/reject/{approvalId}")
    public ResponseEntity<?> rejectApproval(@PathVariable UUID approvalId,
                                            @RequestHeader ("X-User-MemberPositionId") UUID memberPositionId,
                                            @RequestBody RejectRequestDto dto
    ) {
        approvalService.rejectApproval(approvalId, memberPositionId, dto);
        return new ResponseEntity<>(ApiResponse.success(approvalId, "반려"), HttpStatus.OK);
    }

    @PostMapping("/create-reply/{approvalId}")
    public ResponseEntity<?> createReply(@PathVariable UUID approvalId,
                                         @RequestBody ReplyRequestDto dto,
                                         @RequestHeader("X-User-MemberPositionId") UUID memberPositionId
    ) {
        UUID replyId = approvalService.createReply(approvalId, dto, memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(replyId, "댓글 생성"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/find-reply/{approvalId}")
    ResponseEntity<?> getReply(@PathVariable UUID approvalId,
                               @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC)
                               Pageable pageable
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(approvalService.getReply(approvalId, pageable), "댓글 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-approval/{id}")
    public ResponseEntity<?> findApproval(@PathVariable UUID id) {
        ApprovalResponseDto approval = approvalService.getApproval(id);
        return new ResponseEntity<>(
                ApiResponse.success(approval, "결재 상세 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-approval-list")
    public ResponseEntity<?> findApprovalList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                              @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC)
                                              Pageable pageable
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(approvalService.getApprovalList(memberPositionId, pageable), "결재내역 리스트 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-draft-list")
    public ResponseEntity<?> findDraftList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                           @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC)
                                           Pageable pageable
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(approvalService.getDraftApprovalList(memberPositionId, pageable), "임시저장 리스트 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-complete-list")
    public ResponseEntity<?> findCompleteList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                              @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC)
                                              Pageable pageable
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(approvalService.getCompletedApprovalList(memberPositionId, pageable), "결재 완료 리스트 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-approve-complete-list")
    public ResponseEntity<?> findCompleteApproveList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                     @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC)
                                                     Pageable pageable
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(approvalService.getCompletedApproveList(memberPositionId, pageable), "결재 완료 리스트 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-pending-list")
    public ResponseEntity<?> findPendingList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                             @PageableDefault(size = 4, sort = "a.createdAt", direction = Sort.Direction.DESC)
                                             Pageable pageable
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(approvalService.getRequestedApprovalList(memberPositionId, pageable), "승인 대기 리스트 조회"),
                HttpStatus.OK
        );
    }

    @PatchMapping("/attachment/{approvalId}")
    public ResponseEntity<?> patchFile(@PathVariable UUID approvalId,
                                       @RequestPart("attachmentInfo") AttachmentRequestDto dto,
                                       @RequestPart(value = "newFiles", required = false) List<MultipartFile> newFiles)
    {
        approvalService.patchFile(approvalId, dto, newFiles);
        return new ResponseEntity<>(
                ApiResponse.success(approvalId, "첨부파일 추가 및 수정"),
                HttpStatus.OK
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        StatsResponseDto dto = approvalService.getStats(memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(dto, "통계 조회"),
                HttpStatus.OK
        );
    }

    @PutMapping("/document-policy/{documentId}")
    public ResponseEntity<?> updateDocument(@PathVariable UUID documentId,
                                            @RequestBody List<DocumentPolicyDto> dtoList
    ) {
        approvalService.setPolicies(documentId, dtoList);
        return new ResponseEntity<>(
                ApiResponse.success(documentId, "문서 양식 수정"),
                HttpStatus.OK
        );
    }

}
