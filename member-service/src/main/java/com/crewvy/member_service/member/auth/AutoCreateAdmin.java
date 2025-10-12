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
import java.util.stream.Collectors;

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
    private final RolePermissionRepository rolePermissionRepository;

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

        List<Permission> permissionList = new ArrayList<>();

        List<String> sysResourceList = Arrays.asList("member", "title", "grade", "role", "organization", "attendance", "payroll", "system");
        for (String resource : sysResourceList) {
            for (Action action : Action.values()) {
                Permission sysPermission = Permission.builder()
                        .name(String.format("%s:%s:SYSTEM", resource, action))
                        .resource(resource).action(action).permissionRange(PermissionRange.SYSTEM)
                        .description(String.format("[%s]에 대한 %s 최상위 권한", resource, action.getCodeName())).build();
                permissionList.add(sysPermission);
            }
        }

        List<String> adminResourceList = Arrays.asList("member", "title", "grade", "role", "organization", "attendance", "payroll");
        for (String resource : adminResourceList) {
            for (Action action : Action.values()) {
                Permission comPermission = Permission.builder()
                        .name(String.format("%s:%s:COMPANY", resource, action))
                        .resource(resource).action(action).permissionRange(PermissionRange.COMPANY)
                        .description(String.format("[%s]에 대한 %s 회사 전체 권한", resource, action.getCodeName())).build();
                permissionList.add(comPermission);
            }
        }

        List<String> defaultResourceList = Arrays.asList("member", "organization", "attendance", "payroll");
        for (String resource : defaultResourceList) {
            for (Action action : Action.values()) {
                Permission comPermission = Permission.builder()
                        .name(String.format("%s:%s:COMPANY", resource, action))
                        .resource(resource).action(action).permissionRange(PermissionRange.INDIVIDUAL)
                        .description(String.format("[%s]에 대한 %s 회사 전체 권한", resource, action.getCodeName())).build();
                permissionList.add(comPermission);
            }
        }
        permissionRepository.saveAll(permissionList);
    }

    private void createCompanyAndEmployeesViaService() {
        // 회사 관리자(대표) 계정 생성
        CreateAdminReq adminReq = CreateAdminReq.builder()
                .companyName("H.ONE 컴퍼니").businessNumber("123-45-67890")
                .email("admin@h.one").password("12341234").checkPw("12341234")
                .name("김대표").build();
        UUID adminId = onboardingService.createAdminAndInitialSetup(adminReq);
        Member admin = memberRepository.findById(adminId).orElseThrow();
        Company company = admin.getCompany();
        UUID adminPositionId = admin.getDefaultMemberPosition().getId();
        Organization topLevelOrg = admin.getDefaultMemberPosition().getOrganization();

        // 시스템 관리자 계정 생성
        Role systemAdminRole = Role.builder()
                .name("System Admin")
                .company(company)
                .build();
        roleRepository.save(systemAdminRole);

        List<Permission> systemAdminPermissions = new ArrayList<>();
        List<String> systemResources = Arrays.asList("member", "title", "grade", "role", "organization", "attendance", "payroll", "system");
        List<Action> systemActions = Arrays.asList(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE);

        for (String resource : systemResources) {
            for (Action action : systemActions) {
                permissionRepository.findByResourceAndActionAndPermissionRange(resource, action, PermissionRange.SYSTEM)
                        .ifPresent(systemAdminPermissions::add);
            }
        }

        List<RolePermission> systemAdminRolePermissions = systemAdminPermissions.stream()
                .map(permission -> RolePermission.builder()
                        .role(systemAdminRole)
                        .permission(permission)
                        .build())
                .collect(Collectors.toList());
        rolePermissionRepository.saveAll(systemAdminRolePermissions);
        systemAdminRole.updatePermission(systemAdminRolePermissions);
        roleRepository.save(systemAdminRole);

        Title systemAdminTitle = Title.builder()
                .name("System Admin Title")
                .company(company)
                .build();
        titleRepository.save(systemAdminTitle);

        CreateMemberReq systemAdminMemberReq = CreateMemberReq.builder()
                .email("sysadmin@h.one")
                .password("sysadmin1234")
                .name("시스템관리자")
                .organizationId(topLevelOrg.getId())
                .titleId(systemAdminTitle.getId())
                .roleId(systemAdminRole.getId())
                .gradeId(null)
                .employmentType(EmploymentType.FULL.getCodeValue())
                .sabun("SYS0001")
                .phoneNumber("010-0000-0000")
                .joinDate(LocalDate.now())
                .profileUrl(null)
                .build();
        memberService.createMember(adminId, adminPositionId, systemAdminMemberReq);

        // 하위 조직 생성 (인사팀, 개발팀)
        CreateOrganizationReq hrReq = new CreateOrganizationReq(topLevelOrg.getId(), "인사팀");
        UUID hrTeamId = organizationService.createOrganization(adminId, adminPositionId, hrReq);

        CreateOrganizationReq devReq = new CreateOrganizationReq(topLevelOrg.getId(), "개발팀");
        UUID devTeamId = organizationService.createOrganization(adminId, adminPositionId, devReq);

        Title teamMemberTitle = titleRepository.save(Title.builder().name("팀원").company(company).build());
        Role userRole = roleRepository.findByNameAndCompany("일반 사용자", company).orElseThrow();

        List<Permission> userPermissionList = new ArrayList<>();
        List<String> userResources = Arrays.asList("member", "organization", "attendance", "payroll");
        List<Action> userActions = Arrays.asList(Action.READ);

        for (String resource : userResources) {
            for (Action action : userActions) {
                permissionRepository.findByResourceAndActionAndPermissionRange(resource, action, PermissionRange.INDIVIDUAL)
                        .ifPresent(userPermissionList::add);
            }
        }

        List<RolePermission> userRolePermissions = userPermissionList.stream()
                .map(permission -> RolePermission.builder()
                        .role(userRole)
                        .permission(permission)
                        .build())
                .collect(Collectors.toList());
        rolePermissionRepository.saveAll(userRolePermissions);
        userRole.updatePermission(userRolePermissions);
        roleRepository.save(userRole);

        Grade defaultGrade = gradeRepository.findByNameAndCompany(company.getCompanyName() + " 관리자", company).orElseThrow();

        // 일반 직원 10명 생성 및 배정
        List<String> names = Arrays.asList("김민준", "이서준", "박도윤", "최시우", "정하준", "강지호", "윤은우", "임선우", "한유찬", "오이안");
        for (int i = 0; i < names.size(); i++) {
            UUID teamId = (i < 5) ? hrTeamId : devTeamId;
            String sabun = String.format("2025%04d", i + 1);
            String phoneNumber = "010-1234-" + String.format("%04d", i + 1);

            CreateMemberReq memberReq = CreateMemberReq.builder()
                    .email("emp" + (i + 1) + "@h.one")
                    .password("12341234")
                    .name(names.get(i))
                    .organizationId(teamId)
                    .titleId(teamMemberTitle.getId())
                    .roleId(userRole.getId())
                    .gradeId(defaultGrade.getId())
                    .employmentType(EmploymentType.FULL.getCodeValue())
                    .sabun(sabun)
                    .phoneNumber(phoneNumber)
                    .joinDate(LocalDate.now())
                    .address("서울시 강남구 테헤란로 " + (100 + i) + "번길")
                    .bank("국민은행")
                    .bankAccount("123456-78-" + String.format("%05d", i + 1))
                    .profileUrl(null)
                    .build();

            memberService.createMember(adminId, adminPositionId, memberReq);
        }
    }
}
