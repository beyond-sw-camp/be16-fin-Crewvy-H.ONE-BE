package com.crewvy.member_service.member.controller;

import com.crewvy.member_service.common.dto.ApiResponse;
import com.crewvy.member_service.member.dto.request.CreateAdminReqDto;
import com.crewvy.member_service.member.dto.request.CreateMemberReqDto;
import com.crewvy.member_service.member.dto.request.LoginReqDto;
import com.crewvy.member_service.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 관리자 계정 생성
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@ModelAttribute @Valid CreateAdminReqDto createAdminReqDto) {
        return new ResponseEntity<>(new ApiResponse(
                true, memberService.createAdmin(createAdminReqDto), "계정 생성 성공"), HttpStatus.CREATED);
    }

    // 사용자 계정 생성
    @PostMapping("/create")
    public ResponseEntity<?> createMember(@ModelAttribute @Valid CreateMemberReqDto createMemberRequestDto) {
        return new ResponseEntity<>(new ApiResponse(
                true, memberService.createMember(createMemberRequestDto), "계정 생성 성공"), HttpStatus.CREATED);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> memberDoLogin(@RequestBody @Valid LoginReqDto loginReqDto) {
        return new ResponseEntity<>(new ApiResponse(
                true, memberService.doLogin(loginReqDto), "로그인 성공"), HttpStatus.OK);
    }

//    // AT 재발급
//    @PostMapping("/generate-at")
//    public ResponseEntity<?> generateNewAt(@RequestBody GenerateNewAtDto generateNewAtDto) {
//        return new ResponseEntity<>(new ApiResponse(
//                true, memberService.generateNewAt(generateNewAtDto), "Access token 재발급 성공"), HttpStatus.OK);
//    }
}
