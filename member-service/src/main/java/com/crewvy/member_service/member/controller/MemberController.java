package com.crewvy.member_service.member.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.constant.PermissionRange;
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
                memberService.emailExist(email), "이메일 중복 확인 완료"), HttpStatus.OK);
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

    // 직원 리스트 조회
    @GetMapping("/list")
    public ResponseEntity<?> memberList(@RequestHeader("X-User-UUID") UUID uuid,
                                        @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getMemberList(uuid, memberPositionId), "직원 목록 조회 성공"), HttpStatus.OK);
    }

    // 직원 정보 수정 페이지
    @GetMapping("/{memberId}/editpage")
    public ResponseEntity<?> getMemberEditPage(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                               @PathVariable UUID memberId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getMemberEditPage(memberPositionId, memberId), "직원 수정 정보 조회 성공"), HttpStatus.OK);
    }

    // 직원 정보 수정
    @PutMapping("/{memberId}/update")
    public ResponseEntity<?> updateMember(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @PathVariable UUID memberId,
                                          @RequestBody @Valid UpdateMemberReq updateMemberReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.updateMember(memberPositionId, memberId, updateMemberReq), "직원 정보 수정 성공"), HttpStatus.OK);
    }

    // 직원 삭제
    @DeleteMapping("/{memberId}/delete")
    public ResponseEntity<?> deleteMember(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @PathVariable UUID memberId) {
        memberService.deleteMember(memberPositionId, memberId);
        return new ResponseEntity<>(ApiResponse.success(null, "직원 정보 삭제 성공"), HttpStatus.OK);
    }

    // 직원 복원
    @PatchMapping("/{memberId}/restore")
    public ResponseEntity<?> restoreMember(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                           @PathVariable UUID memberId) {
        memberService.restoreMember(memberPositionId, memberId);
        return new ResponseEntity<>(ApiResponse.success(null, "직원 정보 복원 성공"), HttpStatus.OK);
    }

    // 직원 상세 조회
    @GetMapping("/detail/{memberId}")
    public ResponseEntity<?> memberList(@RequestHeader("X-User-UUID") UUID uuid,
                                        @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                        @PathVariable UUID memberId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getMemberDetail(uuid, memberPositionId, memberId), "직원 상세 조회 성공"), HttpStatus.OK);
    }

    // 직책 생성
    @PostMapping("/create-title")
    public ResponseEntity<?> createTitle(@RequestHeader("X-User-UUID") UUID uuid,
                                         @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @RequestBody CreateTitleReq createTitleReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.createTitle(uuid, memberPositionId, createTitleReq), "직책 생성 성공"), HttpStatus.CREATED);
    }

    // 직책 목록 조회
    @GetMapping("/title")
    public ResponseEntity<?> getTitle(@RequestHeader("X-User-UUID") UUID uuid, @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getTitle(uuid, memberPositionId), "직책 목록 조회 성공"), HttpStatus.OK);
    }

    // 직책 수정
    @PutMapping("/title/{titleId}")
    public ResponseEntity<?> updateTitle(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @PathVariable UUID titleId,
                                         @RequestBody UpdateTitleReq updateTitleReq) {
        memberService.updateTitle(memberPositionId, titleId, updateTitleReq);
        return new ResponseEntity<>(ApiResponse.success(null, "직책 수정 성공"), HttpStatus.OK);
    }

    // 직책 순서 변경
    @PutMapping("/title/reorder")
    public ResponseEntity<?> reorderTitle(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @RequestBody ReorderReq reorderReq) {
        memberService.reorderTitles(memberPositionId, reorderReq);
        return new ResponseEntity<>(ApiResponse.success(null, "직책 순서 변경 성공"), HttpStatus.OK);
    }

    // 직책 삭제
    @DeleteMapping("/title/{titleId}")
    public ResponseEntity<?> deleteTitle(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @PathVariable UUID titleId) {
        memberService.deleteTitle(memberPositionId, titleId);
        return new ResponseEntity<>(ApiResponse.success(null, "직책 삭제 성공"), HttpStatus.OK);
    }

    // 직책 복원
    @PatchMapping("/title/{titleId}/restore")
    public ResponseEntity<?> restoreTitle(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @PathVariable UUID titleId) {
        memberService.restoreTitle(memberPositionId, titleId);
        return new ResponseEntity<>(ApiResponse.success(null, "직책 복원 성공"), HttpStatus.OK);
    }

    // 직급 생성
    @PostMapping("/create-grade")
    public ResponseEntity<?> createGrade(@RequestHeader("X-User-UUID") UUID uuid,
                                         @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @RequestBody CreateGradeReq createGradeReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.createGrade(uuid, memberPositionId, createGradeReq), "직급 생성 성공"), HttpStatus.CREATED);
    }

    // 직급 목록 조회
    @GetMapping("/grade")
    public ResponseEntity<?> getGrades(@RequestHeader("X-User-UUID") UUID uuid,
                                       @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getGrade(uuid, memberPositionId), "직급 목록 조회 성공"), HttpStatus.OK);
    }

    // 직급 수정
    @PutMapping("/grade/{gradeId}")
    public ResponseEntity<?> updateGrade(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @PathVariable UUID gradeId,
                                         @RequestBody UpdateGradeReq updateGradeReq) {
        memberService.updateGrade(memberPositionId, gradeId, updateGradeReq);
        return new ResponseEntity<>(ApiResponse.success(null, "직급 수정 성공"), HttpStatus.OK);
    }

    // 직급 순서 변경
    @PutMapping("/grade/reorder")
    public ResponseEntity<?> reorderGrade(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @RequestBody ReorderReq reorderReq) {
        memberService.reorderGrades(memberPositionId, reorderReq);
        return new ResponseEntity<>(ApiResponse.success(null, "직급 순서 변경 성공"), HttpStatus.OK);
    }

    // 직급 삭제
    @DeleteMapping("/grade/{gradeId}")
    public ResponseEntity<?> deleteGrade(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @PathVariable UUID gradeId) {
        memberService.deleteGrade(memberPositionId, gradeId);
        return new ResponseEntity<>(ApiResponse.success(null, "직급 삭제 성공"), HttpStatus.OK);
    }

    // 직급 복원
    @PatchMapping("/grade/{gradeId}/restore")
    public ResponseEntity<?> restoreGrade(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @PathVariable UUID gradeId) {
        memberService.restoreGrade(memberPositionId, gradeId);
        return new ResponseEntity<>(ApiResponse.success(null, "직급 복원 성공"), HttpStatus.OK);
    }

    // 역할 생성
    @PostMapping("/role-create")
    public ResponseEntity<?> createRole(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                        @RequestBody @Valid RoleUpdateReq roleUpdateReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.createRole(memberPositionId, roleUpdateReq), "역할 생성 성공"), HttpStatus.CREATED);
    }

    // 역할 목록 조회
    @GetMapping("/role")
    public ResponseEntity<?> getRole(@RequestHeader("X-User-UUID") UUID uuid,
                                     @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getRole(uuid, memberPositionId), "역할 목록 조회 성공"), HttpStatus.OK);
    }

    // 역할 상세 조회
    @GetMapping("/role/{roleId}")
    public ResponseEntity<?> getRoleDetail(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                           @PathVariable UUID roleId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getRoleById(memberPositionId, roleId), "역할 상세 조회 성공"), HttpStatus.OK);
    }

    // 역할 수정
    @PutMapping("/role/{roleId}/update")
    public ResponseEntity<?> updateRole(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                        @PathVariable UUID roleId,
                                        @RequestBody @Valid RoleUpdateReq roleUpdateReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.updateRole(memberPositionId, roleId, roleUpdateReq), "역할 수정 성공"), HttpStatus.OK);
    }

    // 멤버의 역할 변경
    @PutMapping("/position/{targetMemberPositionId}/role")
    public ResponseEntity<?> updateMemberRole(@RequestHeader("X-User-MemberPositionId") UUID adminMemberPositionId,
                                              @PathVariable UUID targetMemberPositionId,
                                              @RequestBody @Valid UpdateMemberRoleReq req) {
        memberService.updateMemberRole(adminMemberPositionId, targetMemberPositionId, req);
        return new ResponseEntity<>(ApiResponse.success(null, "멤버 역할 변경 성공"), HttpStatus.OK);
    }

    // 역할 순서 변경
    @PutMapping("/role/reorder")
    public ResponseEntity<?> reorderRole(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @RequestBody ReorderReq reorderReq) {
        memberService.reorderRoles(memberPositionId, reorderReq);
        return new ResponseEntity<>(ApiResponse.success(null, "역할 순서 변경 성공"), HttpStatus.OK);
    }

    // 역할 삭제
    @DeleteMapping("/role/{roleId}/delete")
    public ResponseEntity<?> deleteRole(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                        @PathVariable UUID roleId) {
        memberService.deleteRole(memberPositionId, roleId);
        return new ResponseEntity<>(ApiResponse.success(null, "역할 삭제 성공"), HttpStatus.OK);
    }

    // 역할 복원
    @PatchMapping("/role/{roleId}/restore")
    public ResponseEntity<?> restoreRole(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @PathVariable UUID roleId) {
        memberService.restoreRole(memberPositionId, roleId);
        return new ResponseEntity<>(ApiResponse.success(null, "역할 복원 성공"), HttpStatus.OK);
    }

    // 마이페이지
    @GetMapping("/mypage")
    public ResponseEntity<?> myPage(@RequestHeader("X-User-UUID") UUID uuid,
                                    @RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.myPage(uuid, memberPositionId), "마이페이지 조회 성공"), HttpStatus.OK);
    }

    // 마이페이지 수정
    @PutMapping("/mypage/update")
    public ResponseEntity<?> updateMyPage(@RequestHeader("X-User-UUID") UUID uuid,
                                          @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                          @RequestBody @Valid MyPageEditReq myPageEditReq) {
        memberService.updateMyPage(uuid, memberPositionId, myPageEditReq);
        return new ResponseEntity<>(ApiResponse.success(null, "마이페이지 수정 성공"), HttpStatus.OK);
    }

    // 권한 확인
    @GetMapping("/check-permission")
    public ResponseEntity<?> checkPermission(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                             @RequestParam String resource,
                                             @RequestParam Action action,
                                             @RequestParam PermissionRange range) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.checkPermission(memberPositionId, resource, action, range), "권한 확인 성공"), HttpStatus.OK);
    }

    // 모든 권한 목록 조회
    @GetMapping("/permission")
    public ResponseEntity<?> getAllPermission() {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getAllPermission(), "전체 권한 목록 조회 성공"), HttpStatus.OK);
    }

    // memberIdList -> 이름 List
    @PostMapping("/name-list")
    public ResponseEntity<?> getNameList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                         @RequestBody IdListReq idListReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getNameList(memberPositionId, idListReq), "목록 조회 성공"), HttpStatus.OK);
    }

    // 조멤직IdList -> ( 이름, 부서, 직급 ) List
    @PostMapping("/position-list")
    public ResponseEntity<?> getPositionList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                             @RequestBody IdListReq idListReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getPositionList(memberPositionId, idListReq), "목록 조회 성공"), HttpStatus.OK);
    }

    // companyId -> ( 사번, 이름, 부서, 직급, 계좌, 은행 ) List
    @GetMapping("/salary-list")
    public ResponseEntity<?> getSalaryList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                           @RequestParam UUID companyId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getSalaryList(memberPositionId, companyId), "목록 조회 성공"), HttpStatus.OK);
    }

    // memberIdList -> defaultMemberPosition의 ( 이름, 부서, 직급 ) List
    @PostMapping("/default-position-list")
    public ResponseEntity<?> getDefaultPositionList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                    @RequestBody IdListReq idListReq) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getDefaultPositionList(memberPositionId, idListReq), "목록 조회 성공"), HttpStatus.OK);
    }

    // memberPositionId -> 조직 List( 0: 내 부서, 1: 상위 부서, 2: 1의 상위 부서, ... , n: 회사 )
    @GetMapping("/organization-list")
    public ResponseEntity<?> getOrganizationList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(ApiResponse.success(
                memberService.getOrganizationList(memberPositionId), "목록 조회 성공"), HttpStatus.OK);
    }
}
