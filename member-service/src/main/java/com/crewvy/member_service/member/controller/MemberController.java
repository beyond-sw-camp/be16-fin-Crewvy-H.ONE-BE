package com.crewvy.member_service.member.controller;

import com.chillex.gooseBumps.common.dto.CommonResponseDto;
import com.chillex.gooseBumps.domain.member.dto.request.*;
import com.chillex.gooseBumps.domain.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 회원가입
    @PostMapping("/create")
    public ResponseEntity<?> signUp(@ModelAttribute @Valid MemberCreateDto memberCreateDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonResponseDto.builder()
                        .result(memberService.saveMemberAndPlayLists(memberCreateDto))
                        .statusCode(HttpStatus.CREATED.value())
                        .statusMessage("회원가입 완료").build());
    }

    // 마이페이지
    @GetMapping("/mypage")
    public ResponseEntity<?> myPage() {
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result(memberService.myPage())
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("마이페이지 조회").build());
    }

    // 회원정보 변경
    @PatchMapping("/update")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateMember(@ModelAttribute @Valid MemberUpdateDto memberUpdateDto) {
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result(memberService.updateMember(memberUpdateDto))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("회원정보 변경 완료").build());
    }

    // 회원 목록
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findAll(@PageableDefault(size = 10, sort = "memberSeq") Pageable pageable, MemberSearchDto memberSearchDto) {
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result(memberService.findAll(pageable, memberSearchDto))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("회원 목록 조회").build());
    }

    // 회원 상세 조회(memberSeq)
    @GetMapping("/detail/{memberSeq}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findByMemberSeq(@PathVariable Long memberSeq) {
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result(memberService.findByMemberSeq(memberSeq))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("회원 상세 조회").build());
    }

    // 회원 탈퇴
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteMember() {
        memberService.deleteMember();
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result("ok")
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("회원탈퇴 완료").build());
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody MemberLoginReqDto memberLoginDto) {
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result(memberService.login(memberLoginDto))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("로그인 완료").build());
    }

    // 비밀번호 재설정
    @PostMapping("/findpw")
    public ResponseEntity<?> resetPw(@Valid @RequestBody FindPwDto findPwDto) {
        memberService.resetPw(findPwDto);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result("ok")
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("비밀번호 재설정 완료").build());
    }

    // AT 토큰 재발급
    @PostMapping("/generate-at")
    public ResponseEntity<?> generateNewAt(@RequestBody GenerateNewAtDto generateNewAtDto) {
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result(memberService.generateNewAt(generateNewAtDto))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("Access token 재발급 완료").build());
    }

    // 구글 로그인(최초 회원가입 포함)
    @PostMapping("/google/login")
    public ResponseEntity<?> googleLogin(@RequestBody ProvideCodeDto provideCodeDto){
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result(memberService.googleLogin(provideCodeDto))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("로그인 완료").build());
    }

    // 카카오 로그인(최초 회원가입 포함)
    @PostMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestBody ProvideCodeDto provideCodeDto){
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result(memberService.kakaoLogin(provideCodeDto))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("로그인 완료").build());
    }

    // 회원정보가 일부 없을 경우 수정
    @PatchMapping("/update-info")
    public ResponseEntity<?> oauthUpdate(@Valid @RequestBody OauthUpdateDto oauthUpdateDto){
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponseDto.builder()
                        .result(memberService.oauthUpdate(oauthUpdateDto))
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("회원정보 수정 완료").build());
    }
}
