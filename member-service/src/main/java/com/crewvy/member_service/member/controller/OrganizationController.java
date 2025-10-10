package com.crewvy.member_service.member.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.member_service.member.dto.request.CreateOrganizationReq;
import com.crewvy.member_service.member.service.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                                                @RequestBody CreateOrganizationReq createOrganizationReq) {
        return new ResponseEntity<>(ApiResponse.success(
                organizationService.createOrganization(uuid, memberPositionId, createOrganizationReq), "조직 생성 성공"), HttpStatus.CREATED);
    }

    // 조직 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getOrganizationList(@RequestHeader("X-User-UUID") UUID uuid,
                                                 @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(ApiResponse.success(
                organizationService.getOrganizationList(uuid, memberPositionId), "조직 목록 조회 성공"), HttpStatus.OK);
    }
}