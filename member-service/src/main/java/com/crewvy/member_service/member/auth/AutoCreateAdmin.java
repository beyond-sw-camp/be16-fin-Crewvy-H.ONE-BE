package com.crewvy.member_service.member.auth;

import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.constant.EmploymentType;
import com.crewvy.member_service.member.constant.PermissionRange;
import com.crewvy.member_service.member.dto.request.CreateAdminReq;
import com.crewvy.member_service.member.dto.request.CreateMemberReq;
import com.crewvy.member_service.member.dto.request.CreateOrganizationReq;
import com.crewvy.member_service.member.entity.*;
import com.crewvy.member_service.member.repository.*;
import com.crewvy.member_service.member.service.MemberService;
import com.crewvy.member_service.member.service.OnboardingService;
import com.crewvy.member_service.member.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@DependsOn("permissionCacheManager")
public class AutoCreateAdmin implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final TitleRepository titleRepository;
    private final GradeRepository gradeRepository;
    private final OnboardingService onboardingService;
    private final MemberService memberService;
    private final OrganizationService organizationService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.count() > 0) {
            return; // 데이터가 이미 있으면 실행하지 않음
        }

        createBasePermissions();
        createCompanyAndEmployeesViaService();
    }

    private void createBasePermissions() {
        if (permissionRepository.count() > 0) {
            return;
        }

        List<Permission> permissions = new ArrayList<>();
        List<String> resources = Arrays.asList("member", "organization", "attendance", "payroll");

        for (String resource : resources) {
            for (Action action : Action.values()) {
                Permission sysPermission = Permission.builder()
                        .name(String.format("%s:%s:SYSTEM", resource, action))
                        .resource(resource).action(action).permissionRange(PermissionRange.SYSTEM)
                        .description(String.format("[%s]에 대한 %s 최상위 권한", resource, action.getCodeName())).build();
                permissions.add(sysPermission);

                Permission comPermission = Permission.builder()
                        .name(String.format("%s:%s:COMPANY", resource, action))
                        .resource(resource).action(action).permissionRange(PermissionRange.COMPANY)
                        .description(String.format("[%s]에 대한 %s 회사 전체 권한", resource, action.getCodeName())).build();
                permissions.add(comPermission);
            }
        }
        permissionRepository.saveAll(permissions);
    }

    private void createCompanyAndEmployeesViaService() {
        // 1. 회사 관리자(대표) 계정 생성
        CreateAdminReq adminReq = CreateAdminReq.builder()
                .companyName("H.ONE 컴퍼니").businessNumber("123-45-67890")
                .email("admin@h.one").password("12341234").checkPw("12341234")
                .name("김대표").build();
        UUID adminId = onboardingService.createAdminAndInitialSetup(adminReq);
        Member admin = memberRepository.findById(adminId).orElseThrow();
        Company company = admin.getCompany();
        UUID adminPositionId = admin.getDefaultMemberPosition().getId();
        Organization topLevelOrg = admin.getDefaultMemberPosition().getOrganization();

        // 2. 하위 조직 생성 (인사팀, 개발팀)
        CreateOrganizationReq hrReq = new CreateOrganizationReq(topLevelOrg.getId(), "인사팀");
        UUID hrTeamId = organizationService.createOrganization(adminId, adminPositionId, hrReq);

        CreateOrganizationReq devReq = new CreateOrganizationReq(topLevelOrg.getId(), "개발팀");
        UUID devTeamId = organizationService.createOrganization(adminId, adminPositionId, devReq);

        // 3. 직책 및 역할 조회/생성
        Title teamMemberTitle = titleRepository.save(Title.builder().name("팀원").company(company).build());
        Role userRole = roleRepository.findByNameAndCompany("일반 사용자", company).orElseThrow();
        Grade defaultGrade = gradeRepository.findByNameAndCompany(company.getCompanyName() + " 관리자", company).orElseThrow(); // 기본 직급 조회

        // 4. 일반 직원 10명 생성 및 배정
        List<String> names = Arrays.asList("김민준", "이서준", "박도윤", "최시우", "정하준", "강지호", "윤은우", "임선우", "한유찬", "오이안");
        for (int i = 0; i < names.size(); i++) {
            UUID teamId = (i < 5) ? hrTeamId : devTeamId;
            String sabun = String.format("2025%04d", i + 1); // 사번 생성
            String phoneNumber = "010-1234-" + String.format("%04d", i + 1);

            CreateMemberReq memberReq = CreateMemberReq.builder()
                    .email("employee" + (i + 1) + "@h.one")
                    .password("12341234")
                    .name(names.get(i))
                    .organizationId(teamId)
                    .titleId(teamMemberTitle.getId())
                    .roleId(userRole.getId())
                    .gradeId(defaultGrade.getId()) // 직급 ID 추가
                    .employmentType(EmploymentType.FULL.getCodeValue())
                    .sabun(sabun)
                    .phoneNumber(phoneNumber)
                    .joinDate(LocalDate.now())
                    .address("서울시 강남구 테헤란로 " + (100 + i) + "번길")
                    .bank("국민은행")
                    .bankAccount("123456-78-" + String.format("%05d", i + 1))
                    .profileUrl("http://example.com/profile/employee" + (i + 1) + ".jpg")
                    .build();

            memberService.createMember(adminId, adminPositionId, memberReq);
        }
    }
}