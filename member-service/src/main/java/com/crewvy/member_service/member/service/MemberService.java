package com.crewvy.member_service.member.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.auth.JwtTokenProvider;
import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.dto.request.*;
import com.crewvy.member_service.member.dto.response.LoginRes;
import com.crewvy.member_service.member.entity.*;
import com.crewvy.member_service.member.repository.*;
import jakarta.ws.rs.ForbiddenException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MemberService {

    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final GradeRepository gradeRepository;
    private final TitleRepository titleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public MemberService(CompanyRepository companyRepository, MemberRepository memberRepository,
                         MemberPositionRepository memberPositionRepository, RoleRepository roleRepository,
                         PermissionRepository permissionRepository, OrganizationRepository organizationRepository,
                         GradeRepository gradeRepository, TitleRepository titleRepository,
                         PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.companyRepository = companyRepository;
        this.memberRepository = memberRepository;
        this.memberPositionRepository = memberPositionRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.organizationRepository = organizationRepository;
        this.gradeRepository = gradeRepository;
        this.titleRepository = titleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 관리자 계정 생성
    public UUID createAdmin(CreateAdminReq createAdminReq) {
        if (memberRepository.findByEmail(createAdminReq.getEmail()).isPresent()) {
            throw new IllegalArgumentException("사용할 수 없는 이메일입니다.");
        }

        if (!createAdminReq.getPassword().equals(createAdminReq.getCheckPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String encodePassword = passwordEncoder.encode(createAdminReq.getPassword());

        Company company = createCompany(createAdminReq.getCompanyName());
        Organization organization = createDefaultOrganization(company);
        Role adminRole = createAdminRole(company);
        createBaseRole(company);
        Grade grade = createDefaultGrade(company);
        Title adminTitle = createDefaultTitle(company);

        Member adminMember = createAdminReq.toEntity(encodePassword, company);
        memberRepository.save(adminMember);

        MemberPosition adminPosition = createMemberPosition(adminMember, organization, adminTitle, adminRole, LocalDateTime.now());
        adminMember.updateDefaultMemberPosition(adminPosition);
        memberRepository.save(adminMember);

        return adminMember.getId();
    }

    // 사용자 계정 생성
    public UUID createMember(UUID memberId, UUID memberPositionId, CreateMemberReq createMemberReq) {
        if (checkPermission(memberPositionId, "member", Action.CREATE).equals(Bool.FALSE)) {
            throw new ForbiddenException("권한이 없습니다.");
        }

        if (memberRepository.findByEmail(createMemberReq.getEmail()).isPresent()) {
            throw new IllegalArgumentException("사용할 수 없는 이메일입니다.");
        }

        Member admin = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = admin.getCompany();
        String encodePassword = passwordEncoder.encode(createMemberReq.getPassword());
        Member savedMember = memberRepository.save(createMemberReq.memberToEntity(encodePassword, company));

        Organization organization = organizationRepository.findById(createMemberReq.getOrganizationId()).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 조직입니다."));

        Title title = titleRepository.findById(createMemberReq.getTitleId()).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 직책입니다."));

        Role role = roleRepository.findById(createMemberReq.getRoleId()).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 역할입니다."));

        MemberPosition memberPosition = createMemberPosition(savedMember, organization, title, role, LocalDateTime.now());
        savedMember.updateDefaultMemberPosition(memberPosition);
        return savedMember.getId();
    }

    // 로그인
    public LoginRes doLogin(LoginReq loginReq) {
        Member member = memberRepository.findByEmail(loginReq.getEmail()).orElseThrow(()
                -> new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(loginReq.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다.");
        }

        if (member.getYnDel().equals(Bool.TRUE)) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        String accessToken = jwtTokenProvider.createAtToken(member, member.getDefaultMemberPosition());
//        String refreshToken = jwtTokenProvider.createRtToken(member);
        return LoginRes.builder()
                .accessToken(accessToken)
//                .refreshToken(refreshToken)
                .build();
    }

    // 조직 생성
    public UUID createOrganization(UUID memberId, UUID memberPositionId, CreateOrganizationReq createOrganizationReq) {
        if (checkPermission(memberPositionId, "member", Action.CREATE).equals(Bool.FALSE)) {
            throw new ForbiddenException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Organization parent = organizationRepository.findById(createOrganizationReq.getParentId()).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 상위 조직입니다."));
        return organizationRepository.save(createOrganizationReq.toEntity(parent, member.getCompany())).getId();
    }

    // 직책 생성
    public UUID createTitle(UUID memberId, UUID memberPositionId, CreateTitleReq createTitleReq) {
        if (checkPermission(memberPositionId, "member", Action.CREATE).equals(Bool.FALSE)) {
            throw new ForbiddenException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        return titleRepository.save(createTitleReq.toEntity(member.getCompany())).getId();
    }

    // 직급 생성
    public UUID createGrade(UUID memberId, UUID memberPositionId, CreateGradeReq createGradeReq) {
        if (checkPermission(memberPositionId, "member", Action.CREATE).equals(Bool.FALSE)) {
            throw new ForbiddenException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        return gradeRepository.save(createGradeReq.toEntity(member.getCompany())).getId();
    }

    // 권한 확인
    public Bool checkPermission(UUID memberPositionId, String resource, Action action) {
        if (!permissionRepository.hasPermission(memberPositionId, resource, action)) {
            return Bool.FALSE;
        } else {
            return Bool.TRUE    ;
        }
    }

    // MemberPosition 생성
    private MemberPosition createMemberPosition(Member member, Organization organization,
                                                Title title, Role role, LocalDateTime startDate) {
        MemberPosition memberPosition = MemberPosition.builder()
                .member(member)
                .organization(organization)
                .title(title)
                .role(role)
                .startDate(startDate)
                .endDate(null)
                .build();
        return memberPositionRepository.save(memberPosition);
    }

    // 회사 생성
    private Company createCompany(String companyName) {
        Company company = Company.builder().companyName(companyName).build();
        return companyRepository.save(company);
    }

    // 최초 조직 생성
    private Organization createDefaultOrganization(Company company) {
        Organization organization = Organization.builder()
                .parent(null)
                .name(company.getCompanyName())
                .company(company)
                .children(null)
                .build();
        return organizationRepository.save(organization);
    }

    // 관리자
    private Role createAdminRole(Company company) {
        Role adminRole = Role.builder()
                .name(company.getCompanyName() + " 관리자")
                .company(company)
                .build();
        roleRepository.save(adminRole);

        List<Action> actions = Arrays.asList(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE);
        List<RolePermission> permissions = actions.stream()
                .map(action -> {
                    Permission permission = permissionRepository.findByResourceAndAction("member", action)
                            .orElseThrow(() -> new IllegalStateException("존재하지 않는 권한입니다. (자원: member, 작업: " + action + ")"));
                    return RolePermission.builder()
                            .role(adminRole)
                            .permission(permission)
                            .build();
                }).collect(Collectors.toList());

        adminRole.updatePermission(permissions);
        return adminRole;
    }

    // 일반 사용자 역할 생성
    private Role createBaseRole(Company company) {
        Role generalRole = Role.builder()
                .name("일반 사용자")
                .company(company)
                .build();
        return roleRepository.save(generalRole);
    }

    // 관리자 직급 생성
    private Grade createDefaultGrade(Company company) {
        Grade grade = Grade.builder().name(company.getCompanyName() + " 관리자").company(company).build();
        return gradeRepository.save(grade);
    }

    // 관리자 직책 생성
    private Title createDefaultTitle(Company company) {
        Title title = Title.builder().name(company.getCompanyName() + " 관리자").company(company).build();
        return titleRepository.save(title);
    }


}