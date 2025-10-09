package com.crewvy.member_service.member.auth;

import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.constant.EmploymentType;
import com.crewvy.member_service.member.constant.PermissionRange;
import com.crewvy.member_service.member.entity.*;
import com.crewvy.member_service.member.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AutoCreateAdmin implements ApplicationRunner {

    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final GradeRepository gradeRepository;
    private final TitleRepository titleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.count() > 0) {
            return;
        }

        createBasePermissions();
        createSystemAdmin();
        createCompanyWithEmployees();
    }

    private void createBasePermissions() {
        if (permissionRepository.count() > 0) {
            return;
        }

        List<Permission> permissions = new ArrayList<>();
        String resource = "member";

        for (Action action : Action.values()) {
            for (PermissionRange range : PermissionRange.values()) {
                String name = String.format("%s:%s:%s", resource, action, range);
                Permission permission = Permission.builder()
                        .name(name)
                        .resource(resource)
                        .action(action)
                        .permissionRange(range)
                        .description(String.format("%s에 대한 %s(%s) 권한", resource, action.name(), range.getCodeName()))
                        .build();
                permissions.add(permission);
            }
        }
        permissionRepository.saveAll(permissions);
    }

    private void createSystemAdmin() {
        Company sysCompany = createCompany("SYSTEM", "000-00-00000");
        Organization sysOrg = createDefaultOrganization(sysCompany);
        Title sysTitle = createDefaultTitle(sysCompany, "시스템 관리자");

        List<Permission> allPermissions = permissionRepository.findAll();
        Role sysAdminRole = createRoleAndAssignPermissions(sysCompany, "시스템 관리자", allPermissions);

        Member systemAdmin = createMember(sysCompany, "sysadmin@h.one", "12341234", "시스템관리자");
        memberRepository.save(systemAdmin);

        MemberPosition sysAdminPos = createMemberPosition(systemAdmin, sysOrg, sysTitle, sysAdminRole);
        systemAdmin.updateDefaultMemberPosition(sysAdminPos);
        memberRepository.save(systemAdmin);
    }

    private void createCompanyWithEmployees() {
        Company company = createCompany("H.ONE Company", "123-45-67890");
        Organization topLevelOrg = createDefaultOrganization(company);
        Grade defaultGrade = createDefaultGrade(company, "사원");
        Title adminTitle = createDefaultTitle(company, "대표");
        Title defaultTitle = createDefaultTitle(company, "팀원");

        List<Permission> companyAdminPermissions = permissionRepository.findByPermissionRange(PermissionRange.COMPANY);
        Role companyAdminRole = createRoleAndAssignPermissions(company, "회사 관리자", companyAdminPermissions);

        List<Permission> userPermissions = permissionRepository.findByPermissionRange(PermissionRange.INDIVIDUAL);
        Role userRole = createRoleAndAssignPermissions(company, "일반 사용자", userPermissions);

        Member companyAdmin = createMember(company, "admin@h.one", "12341234", "김대표");
        memberRepository.save(companyAdmin);

        MemberPosition adminPosition = createMemberPosition(companyAdmin, topLevelOrg, adminTitle, companyAdminRole);
        companyAdmin.updateDefaultMemberPosition(adminPosition);
        memberRepository.save(companyAdmin);

        List<Member> employeesToSave = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String email = "employee" + i + "@h.one";
            String name = "직원" + i;
            Member employee = createMember(company, email, "12341234", name);
            memberRepository.save(employee);

            MemberPosition employeePosition = createMemberPosition(employee, topLevelOrg, defaultTitle, userRole);
            employee.updateDefaultMemberPosition(employeePosition);
            employeesToSave.add(employee);
        }
        memberRepository.saveAll(employeesToSave);
    }

    private Role createRoleAndAssignPermissions(Company company, String roleName, List<Permission> permissions) {
        Role role = Role.builder().name(roleName).company(company).build();
        roleRepository.save(role);

        List<RolePermission> rolePermissions = new ArrayList<>();
        for (Permission permission : permissions) {
            RolePermission rolePermission = RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .build();
            rolePermissions.add(rolePermission);
        }
        if (!rolePermissions.isEmpty()) {
            rolePermissionRepository.saveAll(rolePermissions);
        }
        role.updatePermission(rolePermissions);
        return role;
    }

    private Company createCompany(String companyName, String businessNumber) {
        Company company = Company.builder().companyName(companyName).businessNumber(businessNumber).build();
        return companyRepository.save(company);
    }

    private Organization createDefaultOrganization(Company company) {
        Organization organization = Organization.builder()
                .parent(null)
                .name(company.getCompanyName())
                .company(company)
                .children(null)
                .build();
        return organizationRepository.save(organization);
    }

    private Grade createDefaultGrade(Company company, String gradeName) {
        Grade grade = Grade.builder()
                .name(gradeName)
                .company(company)
                .build();
        return gradeRepository.save(grade);
    }

    private Title createDefaultTitle(Company company, String titleName) {
        Title title = Title.builder()
                .name(titleName)
                .company(company)
                .build();
        return titleRepository.save(title);
    }

    private Member createMember(Company company, String email, String password, String name) {
        return Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .employmentType(EmploymentType.FULL)
                .company(company).build();
    }

    private MemberPosition createMemberPosition(Member member, Organization organization, Title title, Role role) {
        MemberPosition memberPosition = MemberPosition.builder().member(member).organization(organization).title(title).role(role).startDate(LocalDateTime.now()).build();
        return memberPositionRepository.save(memberPosition);
    }
}
