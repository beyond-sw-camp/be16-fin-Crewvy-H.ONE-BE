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
public class AutoCreateTestData implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final TitleRepository titleRepository;
    private final GradeRepository gradeRepository;
    private final OnboardingService onboardingService;
    private final MemberService memberService;
    private final OrganizationService organizationService;
    private final RolePermissionRepository rolePermissionRepository;
    private final GradeHistoryRepository gradeHistoryRepository;

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

        List<String> sysResourceList = Arrays.asList("member", "title", "grade", "role", "organization", "attendance", "attendance-policy", "salary", "approval", "system");
        for (String resource : sysResourceList) {
            for (Action action : Action.values()) {
                Permission sysPermission = Permission.builder()
                        .name(String.format("%s:%s:SYSTEM", resource, action))
                        .resource(resource)
                        .action(action)
                        .permissionRange(PermissionRange.SYSTEM)
                        .description(String.format("[%s]에 대한 %s 권한", resource, action.getCodeName())).build();
                permissionList.add(sysPermission);
            }
        }

        List<String> adminResourceList = Arrays.asList("member", "title", "grade", "role", "organization", "attendance", "attendance-policy", "salary", "approval");
        for (String resource : adminResourceList) {
            for (Action action : Action.values()) {
                Permission adminPermission = Permission.builder()
                        .name(String.format("%s:%s:COMPANY", resource, action))
                        .resource(resource)
                        .action(action)
                        .permissionRange(PermissionRange.COMPANY)
                        .description(String.format("[%s]에 대한 %s 권한", resource, action.getCodeName())).build();
                permissionList.add(adminPermission);
            }
        }

        List<String> departmentResourceList = Arrays.asList("member");
        for (String resource : departmentResourceList) {
            for (Action action : Action.values()) {
                Permission departmentPermission = Permission.builder()
                        .name(String.format("%s:%s:DEPARTMENT", resource, action))
                        .resource(resource)
                        .action(action)
                        .permissionRange(PermissionRange.DEPARTMENT)
                        .description(String.format("[%s]에 대한 %s 권한", resource, action.getCodeName())).build();
                permissionList.add(departmentPermission);
            }
        }


        List<String> defaultResourceList = Arrays.asList("member", "organization", "attendance", "salary");
        for (String resource : defaultResourceList) {
            for (Action action : Action.values()) {
                Permission comPermission = Permission.builder()
                        .name(String.format("%s:%s:INDIVIDUAL", resource, action))
                        .resource(resource)
                        .action(action)
                        .permissionRange(PermissionRange.INDIVIDUAL)
                        .description(String.format("[%s]에 대한 %s 권한", resource, action.getCodeName())).build();
                permissionList.add(comPermission);
            }
        }

        List<String> noneResourceList = Arrays.asList("member", "title", "grade", "role", "organization", "attendance", "attendance-policy", "salary", "approval");
        for (String resource : noneResourceList) {
            for (Action action : Action.values()) {
                Permission nonePermission = Permission.builder()
                        .name(String.format("%s:%s:NONE", resource, action))
                        .resource(resource)
                        .action(action)
                        .permissionRange(PermissionRange.NONE)
                        .description(String.format("[%s]에 대한 %s 권한", resource, action.getCodeName())).build();
                permissionList.add(nonePermission);
            }
        }

        permissionRepository.saveAll(permissionList);
    }

    private void createCompanyAndEmployeesViaService() {
        for (int c = 1; c <= 3; c++) {
            // 회사 관리자(대표) 계정 생성
            CreateAdminReq adminReq = CreateAdminReq.builder()
                    .companyName("H.ONE 컴퍼니 " + c).businessNumber("123-45-6789" + (c - 1))
                    .email("admin" + c + "@h.one").password("12341234").checkPw("12341234")
                    .name("김대표" + c).build();
            UUID adminId = onboardingService.createAdminAndInitialSetup(adminReq);
            Member admin = memberRepository.findById(adminId).orElseThrow();

            admin = admin.toBuilder()
                    .phoneNumber("010-1111-111" + c)
                    .emergencyContact("010-2222-222" + c)
                    .address("서울시 강남구")
                    .bank("H.ONE 은행")
                    .bankAccount("111-222-33333" + c)
                    .profileUrl(null)
                    .sabun("ADMIN000" + c)
                    .joinDate(LocalDate.now())
                    .extensionNumber("100" + c)
                    .telNumber("02-111-111" + c)
                    .employmentType(EmploymentType.FULL)
                    .build();
            memberRepository.save(admin);

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
            List<String> systemResources = Arrays.asList("member", "title", "grade", "role", "organization", "attendance", "attendance-policy", "salary", "approval", "system");
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
                            .permissionRange(permission.getPermissionRange())
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

            Grade defaultGrade = gradeRepository.findByNameAndCompany(company.getCompanyName() + " 관리자", company).orElseThrow();

            CreateMemberReq systemAdminMemberReq = CreateMemberReq.builder()
                    .email("sysadmin" + c + "@h.one")
                    .password("sysadmin1234")
                    .name("시스템관리자" + c)
                    .organizationId(topLevelOrg.getId())
                    .titleId(systemAdminTitle.getId())
                    .roleId(systemAdminRole.getId())
                    .gradeId(defaultGrade.getId())
                    .employmentType(EmploymentType.FULL.getCodeValue())
                    .sabun("SYS000" + c)
                    .phoneNumber("010-0000-000" + c)
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
            List<String> userResources = Arrays.asList("member", "organization", "attendance", "salary");
            List<Action> userActions = Arrays.asList(Action.READ);

            for (String resource : userResources) {
                for (Action action : userActions) {
                    permissionRepository.findByResourceAndActionAndPermissionRange(resource, action, PermissionRange.INDIVIDUAL)
                            .ifPresent(userPermissionList::add);
                }
            }
            permissionRepository.findByResourceAndActionAndPermissionRange("member", Action.UPDATE, PermissionRange.INDIVIDUAL)
                    .ifPresent(userPermissionList::add);

            List<RolePermission> userRolePermissions = userPermissionList.stream()
                    .map(permission -> RolePermission.builder()
                            .role(userRole)
                            .permission(permission)
                            .permissionRange(permission.getPermissionRange())
                            .build())
                    .collect(Collectors.toList());
            rolePermissionRepository.saveAll(userRolePermissions);
            userRole.updatePermission(userRolePermissions);
            roleRepository.save(userRole);

            Grade employeeGrade = gradeRepository.save(Grade.builder().name("사원").company(company).build());

            // 일반 직원 10명 생성 및 배정 (연차 테스트용 다양한 입사일 설정)
            List<String> names = Arrays.asList("김신규", "박월급", "이반월", "정삼년", "최오년", "황칠년", "문휴가", "서퇴근", "한유찬", "오이안");
            // 입사일 시나리오: 2개월, 9개월, 9개월, 3년, 5년, 7년, 2년, 20개월, 8개월, 7개월
            // 1년 미만 직원들은 전월(10월) 근태 기록을 갖도록 2025년 2~9월 입사로 설정
            List<LocalDate> joinDates = Arrays.asList(
                    LocalDate.of(2025, 9, 15),         // 김신규: 9월 입사 (2개월 전) - 신규 입사자, 10월 근태 O
                    LocalDate.of(2025, 2, 7),          // 박월급: 2월 입사 (9개월 전) - 월별 발생 정상, 10월 근태 O
                    LocalDate.of(2025, 2, 7),          // 이반월: 2월 입사 (9개월 전) - 출근율 테스트용, 10월 근태 O
                    LocalDate.now().minusYears(3),     // 정삼년: 3년차 - 가산 1일
                    LocalDate.now().minusYears(5),     // 최오년: 5년차 - 가산 3일
                    LocalDate.now().minusYears(7),     // 황칠년: 7년차 - 가산 6일
                    LocalDate.now().minusYears(2),     // 문휴가: 2년차 - 분할연차 테스트
                    LocalDate.now().minusMonths(20),   // 서퇴근: 1년 10개월 - 반차/사후신청 테스트
                    LocalDate.of(2025, 3, 10),         // 한유찬: 3월 입사 (8개월 전) - 10월 근태 O
                    LocalDate.of(2025, 4, 20)          // 오이안: 4월 입사 (7개월 전) - 10월 근태 O
            );

            for (int i = 0; i < names.size(); i++) {
                UUID teamId = (i < 5) ? hrTeamId : devTeamId;
                String sabun = String.format("2025%04d", (c - 1) * 10 + i + 1);
                String phoneNumber = "010-1234-" + String.format("%04d", (c - 1) * 10 + i + 1);

                CreateMemberReq memberReq = CreateMemberReq.builder()
                        .email("emp" + c + "_" + (i + 1) + "@h.one")
                        .password("12341234")
                        .name(names.get(i) + c)
                        .organizationId(teamId)
                        .titleId(teamMemberTitle.getId())
                        .roleId(userRole.getId())
                        .gradeId(employeeGrade.getId())
                        .employmentType(EmploymentType.FULL.getCodeValue())
                        .sabun(sabun)
                        .phoneNumber(phoneNumber)
                        .joinDate(joinDates.get(i))
                        .address("서울시 강남구 테헤란로 " + (100 + (c - 1) * 10 + i) + "번길")
                        .bank("국민은행")
                        .bankAccount("123456-78-" + String.format("%06d", (c - 1) * 10 + i + 1))
                        .profileUrl(null)
                        .build();

                memberService.createMember(adminId, adminPositionId, memberReq);
            }
        }
    }
}
