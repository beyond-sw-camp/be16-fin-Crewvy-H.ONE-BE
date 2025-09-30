package com.crewvy.member_service.member.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.dto.request.CreateAdminReq;
import com.crewvy.member_service.member.dto.request.CreateMemberReq;
import com.crewvy.member_service.member.dto.request.LoginReq;
import com.crewvy.member_service.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 관리자 계정 생성
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@ModelAttribute @Valid CreateAdminReq createAdminReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.createAdmin(createAdminReq), "계정 생성 성공"),  HttpStatus.CREATED);
    }

    // 사용자 계정 생성
    @PostMapping("/create")
    public ResponseEntity<?> createMember(@RequestHeader("X-User-UUID") UUID uuid,
                                          @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @ModelAttribute @Valid CreateMemberReq createMemberRequest) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.createMember(uuid, memberPositionId, createMemberRequest), "계정 생성 성공"),  HttpStatus.CREATED);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> memberDoLogin(@RequestBody @Valid LoginReq loginReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.doLogin(loginReq), "로그인 성공"),  HttpStatus.OK);
    }

//    // AT 재발급
//    @PostMapping("/generate-at")
//    public ResponseEntity<?> generateNewAt(@RequestBody GenerateNewAtReq generateNewAtReq) {
//        return new ResponseEntity<>(new ApiResponse(
//                true, memberService.generateNewAt(generateNewAtReq), "Access token 재발급 성공"), HttpStatus.OK);
//    }

    public ResponseEntity<?> checkPermission(UUID memberPositionId, String resource, Action action){
        return new ResponseEntity<>(ApiResponse.success(
                memberService.checkPermission(memberPositionId, resource, action), "권한 확인 성공"),  HttpStatus.OK);
    }
}
