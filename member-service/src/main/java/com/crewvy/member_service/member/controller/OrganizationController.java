package com.crewvy.member_service.member.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.member_service.member.dto.request.CreateOrganizationReq;
import com.crewvy.member_service.member.dto.request.ReorderReq;
import com.crewvy.member_service.member.dto.request.UpdateOrganizationReq;
import com.crewvy.member_service.member.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    // 조직 생성
    @PostMapping("/create")
    public ResponseEntity<?> createOrganization(@RequestHeader("X-User-UUID") UUID uuid,
                                                @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                @Valid @RequestBody CreateOrganizationReq createOrganizationReq) {
        return new ResponseEntity<>(ApiResponse.success(
                organizationService.createOrganization(uuid, memberPositionId, createOrganizationReq), "조직 생성 성공"), HttpStatus.CREATED);
    }

    // 조직 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getOrganizationList(@RequestHeader("X-User-UUID") UUID uuid) {
        return new ResponseEntity<>(ApiResponse.success(
                organizationService.getOrganizationList(uuid), "조직 목록 조회 성공"), HttpStatus.OK);
    }

    // 조직 트리 및 멤버 조회
    @GetMapping("/tree-with-members")
    public ResponseEntity<?> getOrganizationTreeWithMembers(@RequestHeader("X-User-UUID") UUID uuid) {
        return new ResponseEntity<>(ApiResponse.success(
                organizationService.getOrganizationTreeWithMembers(uuid), "조직 트리 및 멤버 조회 성공"), HttpStatus.OK);
    }

    // 조직 이름 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrganization(@RequestHeader("X-User-UUID") UUID uuid,
                                                @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody UpdateOrganizationReq updateOrganizationReq) {
        return new ResponseEntity<>(ApiResponse.success(
                organizationService.updateOrganization(uuid, memberPositionId, id, updateOrganizationReq), "조직 수정 성공"), HttpStatus.OK);
    }

    // 조직 순서 변경
    @PutMapping("/reorder")
    public ResponseEntity<?> reorderOrganization(@RequestBody ReorderReq reorderReq) {
        organizationService.reorderOrganization(reorderReq.getIdList());
        return new ResponseEntity<>(ApiResponse.success(null, "조직 순서 변경 성공"), HttpStatus.OK);
    }

    // 조직 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrganization(@RequestHeader("X-User-UUID") UUID uuid,
                                                @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                @PathVariable UUID id) {
        organizationService.deleteOrganization(uuid, memberPositionId, id);
        return new ResponseEntity<>(ApiResponse.success(null, "조직 삭제 성공"), HttpStatus.OK);
    }
}
