package com.crewvy.member_service.member.auth;

import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.entity.*;
import com.crewvy.member_service.member.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class AutoCreateAdmin implements CommandLineRunner {
    private final CompanyRepository companyRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final GradeRepository gradeRepository;
    private final TitleRepository titleRepository;
    private final MemberPositionRepository memberPositionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (memberRepository.findByEmail("system_admin@naver.com").isPresent()) {
            return;
        }
        // 회사 및 조직 생성
        Company crewvy = Company.builder()
                .companyName("Crewvy")
                .build();
        companyRepository.save(crewvy);

        Organization organization = Organization.builder()
                .parent(null)
                .name("maintenance team")
                .company(crewvy)
                .children(null)
                .build();
        organizationRepository.save(organization);

        // 권한 및 역할 생성
        Role systemAdminRole = Role.builder()
                .name("시스템 관리자")
                .company(crewvy)
                .build();
        roleRepository.save(systemAdminRole);
        List<RolePermission> systemAdminPermission = new ArrayList<>();

        Permission createMemberPermission = Permission.builder()
                .name("계정 생성")
                .resource("member")
                .action(Action.CREATE)
                .description("계정 생성 권한")
                .build();
        systemAdminPermission.add(RolePermission.builder()
                        .role(systemAdminRole)
                        .permission(permissionRepository.save(createMemberPermission))
                .build());

        Permission readMemberPermission = Permission.builder()
                .name("계정 조회")
                .resource("member")
                .action(Action.READ)
                .description("계정 조회 권한")
                .build();
        systemAdminPermission.add(RolePermission.builder()
                .role(systemAdminRole)
                .permission(permissionRepository.save(readMemberPermission))
                .build());

        Permission updateMemberPermission = Permission.builder()
                .name("계정 수정")
                .resource("member")
                .action(Action.UPDATE)
                .description("계정 수정 권한")
                .build();
        systemAdminPermission.add(RolePermission.builder()
                .role(systemAdminRole)
                .permission(permissionRepository.save(updateMemberPermission))
                .build());

        Permission deleteMemberPermission = Permission.builder()
                .name("계정 삭제")
                .resource("member")
                .action(Action.DELETE)
                .description("계정 삭제 권한")
                .build();
        systemAdminPermission.add(RolePermission.builder()
                .role(systemAdminRole)
                .permission(permissionRepository.save(deleteMemberPermission))
                .build());

        systemAdminRole.updatePermission(systemAdminPermission);

        // 직급 생성
        Grade grade = Grade.builder()
                .name("시스템 관리자")
                .company(crewvy)
                .build();
        gradeRepository.save(grade);

        // 직책 생성
        Title title = Title.builder()
                .name("유지보수 담당")
                .company(crewvy)
                .build();
        titleRepository.save(title);

        // member 생성
        Member systemAdmin = Member.builder()
                .email("system_admin@naver.com")
                .password(passwordEncoder.encode("12341234"))
                .name("SystemAdmin")
                .extensionNumber("0000")
                .telNumber("02-000-0000")
                .phoneNumber("010-0000-0000")
                .address("서울시 보라매로 87")
                .sabun("1234")
                .bank("국민")
                .bankAccount("12345-12-12345")
                .company(crewvy)
                .build();
        memberRepository.save(systemAdmin);

        // memberPosition 생성
        MemberPosition memberPosition = MemberPosition.builder()
                .member(systemAdmin)
                .organization(organization)
                .title(title)
                .role(systemAdminRole)
                .startDate(LocalDateTime.now())
                .endDate(null)
                .build();
        memberPositionRepository.save(memberPosition);
        systemAdmin.updateDefaultMemberPosition(memberPosition);
    }
}
