package com.crewvy.member_service.member.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.dto.request.*;
import com.crewvy.member_service.member.service.MemberService;
import com.crewvy.member_service.member.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final OnboardingService onboardingService;

    public MemberController(MemberService memberService, OnboardingService onboardingService) {
        this.memberService = memberService;
        this.onboardingService = onboardingService;
    }

    // 관리자 계정 생성
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@ModelAttribute @Valid CreateAdminReq createAdminReq) {
        return new ResponseEntity<>(ApiResponse.success(
                onboardingService.createAdminAndInitialSetup(createAdminReq), "계정 생성 성공"), HttpStatus.CREATED);
    }

    // 사용자 계정 생성
    @PostMapping("/create")
    public ResponseEntity<?> createMember(@RequestHeader("X-User-UUID") UUID uuid,
                                          @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @ModelAttribute @Valid CreateMemberReq createMemberRequest) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.createMember(uuid, memberPositionId, createMemberRequest), "계정 생성 성공"), HttpStatus.CREATED);
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.emailExists(email), "이메일 중복 확인 완료"), HttpStatus.OK);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> memberDoLogin(@RequestBody @Valid LoginReq loginReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.doLogin(loginReq), "로그인 성공"), HttpStatus.OK);
    }

//    // AT 재발급
//    @PostMapping("/generate-at")
//    public ResponseEntity<?> generateNewAt(@RequestBody GenerateNewAtReq generateNewAtReq) {
//        return new ResponseEntity<>(new ApiResponse(
//                true, memberService.generateNewAt(generateNewAtReq), "Access token 재발급 성공"), HttpStatus.OK);
//    }

    // 직책 생성
    @PostMapping("/create-title")
    public ResponseEntity<?> createTitle(@RequestHeader("X-User-UUID") UUID uuid,
                                         @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @RequestBody CreateTitleReq createTitleReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.createTitle(uuid, memberPositionId, createTitleReq), "직책 생성 성공"), HttpStatus.OK);
    }

    // 직급 생성
    @PostMapping("/create-grade")
    public ResponseEntity<?> createGrade(@RequestHeader("X-User-UUID") UUID uuid,
                                         @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @RequestBody CreateGradeReq createGradeReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.createGrade(uuid, memberPositionId, createGradeReq), "직급 생성 성공"), HttpStatus.OK);
    }

    // 역할의 권한 목록 수정
    @PutMapping("/roles/{roleId}/permissions")
    public ResponseEntity<?> updateRolePermissions(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                   @PathVariable UUID roleId,
                                                   @RequestBody @Valid UpdateRolePermissionsReq req) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.updateRolePermissions(memberPositionId, roleId, req), "역할별 권한 수정 성공"), HttpStatus.OK);
    }

    // 멤버의 역할 변경
    @PutMapping("/positions/{targetMemberPositionId}/role")
    public ResponseEntity<?> updateMemberRole(@RequestHeader("X-User-MemberPositionId") UUID adminMemberPositionId,
                                              @PathVariable UUID targetMemberPositionId,
                                              @RequestBody @Valid UpdateMemberRoleReq req) {
        memberService.updateMemberRole(adminMemberPositionId, targetMemberPositionId, req);
        return new ResponseEntity<>(ApiResponse.success(null, "멤버 역할 변경 성공"), HttpStatus.OK);
    }

    // 직원 리스트 조회
    @GetMapping("/list")
    public ResponseEntity<?> memberList(@RequestHeader("X-User-UUID") UUID uuid,
                                        @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getMemberList(uuid, memberPositionId), "직원 목록 조회 성공"), HttpStatus.OK);
    }

    // 직원 상세 조회
    @GetMapping("/detail/{memberId}")
    public ResponseEntity<?> memberList(@RequestHeader("X-User-UUID") UUID uuid,
                                        @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                        @PathVariable UUID memberId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getMemberDetail(uuid, memberPositionId, memberId), "직원 상세 조회 성공"), HttpStatus.OK);
    }

    // 권한 확인
    @GetMapping("/check-permission")
    public ResponseEntity<?> checkPermission(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                             @RequestParam String resource,
                                             @RequestParam Action action) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.checkPermission(memberPositionId, resource, action), "권한 확인 성공"), HttpStatus.OK);
    }
}
