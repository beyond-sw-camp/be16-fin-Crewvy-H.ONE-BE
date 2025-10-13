package com.crewvy.workforce_service.approval.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.dto.request.UploadDocumentDto;
import com.crewvy.workforce_service.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/system")
public class SystemAdminController {
    private final ApprovalService approvalService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestBody UploadDocumentDto dto) {
        UUID documentId = approvalService.uploadDocument(dto);
        return new ResponseEntity<>(
                ApiResponse.success(documentId, "문서 양식 생성"),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/update-state/{id}")
    public ResponseEntity<?> updateState(@PathVariable UUID id, ApprovalState state) {
        UUID approvalId = approvalService.updateState(id, state);
        return new ResponseEntity<>(
                ApiResponse.success(approvalId, "결재 상태 변경"),
                HttpStatus.OK
        );
    }
}
