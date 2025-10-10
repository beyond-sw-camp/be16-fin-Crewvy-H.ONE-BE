package com.crewvy.member_service.member.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.auth.JwtTokenProvider;
import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.constant.PermissionRange;
import com.crewvy.member_service.member.dto.request.*;
import com.crewvy.member_service.member.dto.response.*;
import com.crewvy.member_service.member.entity.*;
import com.crewvy.member_service.member.repository.*;
import com.crewvy.common.exception.PermissionDeniedException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final GradeRepository gradeRepository;
    private final TitleRepository titleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OrganizationRepository organizationRepository; // createMember에서 필요

    public MemberService(MemberRepository memberRepository,
                         MemberPositionRepository memberPositionRepository, RoleRepository roleRepository,
                         RolePermissionRepository rolePermissionRepository, PermissionRepository permissionRepository,
                         GradeRepository gradeRepository, TitleRepository titleRepository,
                         PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                         OrganizationRepository organizationRepository) {
        this.memberRepository = memberRepository;
        this.memberPositionRepository = memberPositionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.gradeRepository = gradeRepository;
        this.titleRepository = titleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.organizationRepository = organizationRepository;
    }

    // 회원가입
    public Member createAdminMember(CreateAdminReq createAdminReq, Company company) {
        if (memberRepository.findByEmail(createAdminReq.getEmail()).isPresent()) {
            throw new IllegalArgumentException("사용할 수 없는 이메일입니다.");
        }
        if (!createAdminReq.getPassword().equals(createAdminReq.getCheckPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        String encodePassword = passwordEncoder.encode(createAdminReq.getPassword());
        Member adminMember = createAdminReq.toEntity(encodePassword, company);
        return memberRepository.save(adminMember);
    }

    // member_position 생성
    public void createAndAssignDefaultPosition(Member member, Organization organization, Title title, Role role) {
        MemberPosition memberPosition = createMemberPosition(member, organization, title, role, LocalDateTime.now());
        member.updateDefaultMemberPosition(memberPosition);
        memberRepository.save(member);
    }

    // 관리자 역할 생성
    public Role createAdminRole(Company company) {
        Role adminRole = Role.builder()
                .name(company.getCompanyName() + " 관리자")
                .company(company)
                .build();
        roleRepository.save(adminRole);

        List<String> resources = Arrays.asList("member", "organization", "attendance", "payroll");
        List<Action> actions = Arrays.asList(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE);

        List<RolePermission> permissions = resources.stream()
                .flatMap(resource -> actions.stream().map(action -> {
                    Permission permission = permissionRepository.findByResourceAndActionAndPermissionRange(resource, action, PermissionRange.COMPANY)
                            .orElseThrow(() -> new IllegalStateException("존재하지 않는 권한입니다. (자원: " + resource + ", 작업: " + action + ")"));
                    return RolePermission.builder()
                            .role(adminRole)
                            .permission(permission)
                            .build();
                })).collect(Collectors.toList());

        adminRole.updatePermission(permissions);
        return adminRole;
    }

    // 기본 역할 생성
    public Role createBaseRole(Company company) {
        Role generalRole = Role.builder()
                .name("일반 사용자")
                .company(company)
                .build();
        return roleRepository.save(generalRole);
    }

    // 관리자 직급 생성
    public Grade createDefaultGrade(Company company) {
        Grade grade = Grade.builder().name(company.getCompanyName() + " 관리자").company(company).build();
        return gradeRepository.save(grade);
    }

    // 관리자 직책 생성
    public Title createDefaultTitle(Company company) {
        Title title = Title.builder().name(company.getCompanyName() + " 관리자").company(company).build();
        return titleRepository.save(title);
    }

    // 계정 생성
    public UUID createMember(UUID memberId, UUID memberPositionId, CreateMemberReq createMemberReq) {
        if (checkPermission(memberPositionId, "member", Action.CREATE).equals(Bool.FALSE)) {
            throw new PermissionDeniedException("권한이 없습니다.");
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
        return LoginRes.builder().accessToken(accessToken).build();
    }

    // 직책 생성
    public UUID createTitle(UUID memberId, UUID memberPositionId, CreateTitleReq createTitleReq) {
        if (checkPermission(memberPositionId, "member", Action.CREATE).equals(Bool.FALSE)) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        return titleRepository.save(createTitleReq.toEntity(member.getCompany())).getId();
    }

    // 직급 생성
    public UUID createGrade(UUID memberId, UUID memberPositionId, CreateGradeReq createGradeReq) {
        if (checkPermission(memberPositionId, "member", Action.CREATE).equals(Bool.FALSE)) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        return gradeRepository.save(createGradeReq.toEntity(member.getCompany())).getId();
    }

    // 직원 목록 조회
    @Transactional(readOnly = true)
    public List<MemberListRes> getMemberList(UUID memberId, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "member", Action.READ).equals(Bool.FALSE)) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = member.getCompany();

        List<Member> members = memberRepository.findByCompanyWithDetail(company);

        return members.stream()
                .map(Member::getDefaultMemberPosition)
                .filter(java.util.Objects::nonNull)
                .map(MemberListRes::fromEntity)
                .collect(Collectors.toList());
    }

    // 직원 상세 조회
    @Transactional(readOnly = true)
    public MemberDetailRes getMemberDetail(UUID uuid, UUID memberPositionId, UUID memberId) {
        if (checkPermission(memberPositionId, "member", Action.READ).equals(Bool.FALSE)) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Member member = memberRepository.findByIdWithDetail(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        List<TitleRes> titleList = member.getMemberPositionList().stream()
                .map(mp -> TitleRes.fromEntity(mp.getTitle())).distinct().collect(Collectors.toList());

        List<OrganizationRes> organizationList = member.getMemberPositionList().stream()
                .map(mp -> OrganizationRes.fromEntity(mp.getOrganization())).distinct().collect(Collectors.toList());

        List<MemberPositionRes> memberPositionResList = member.getMemberPositionList().stream()
                .map(MemberPositionRes::fromEntity).collect(Collectors.toList());

        return MemberDetailRes.builder()
                .name(member.getName())
                .titleList(titleList)
                .organizationList(organizationList)
                .memberStatus(member.getMemberStatus())
                .accountStatus(member.getAccountStatus())
                .employmentType(member.getEmploymentType())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .emergencyContact(member.getEmergencyContact())
                .address(member.getAddress())
                .bank(member.getBank())
                .bankAccount(member.getBankAccount())
                .profileUrl(member.getProfileUrl())
                .sabun(member.getSabun())
                .joinDate(member.getJoinDate())
                .extensionNumber(member.getExtensionNumber())
                .telNumber(member.getTelNumber())
                .memberPositionResList(memberPositionResList)
                .gradeHistorySet(member.getGradeHistorySet())
                .build();
    }

    // 권한 확인
    @Transactional(readOnly = true)
    public Bool checkPermission(UUID memberPositionId, String resource, Action action) {
        if (!permissionRepository.hasPermission(memberPositionId, resource, action)) {
            return Bool.FALSE;
        } else {
            return Bool.TRUE;
        }
    }

    // 계정 존재여부 확인
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }

    // 역할별 권한 수정
    @CacheEvict(value = "permissions", allEntries = true)
    public UUID updateRolePermissions(UUID memberPositionId, UUID roleId, UpdateRolePermissionsReq req) {
        if (checkPermission(memberPositionId, "member", Action.UPDATE).equals(Bool.FALSE)) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 역할입니다."));

        rolePermissionRepository.deleteAllByRole(role);

        List<Permission> newPermissions = permissionRepository.findAllById(req.getPermissionIds());

        List<RolePermission> newRolePermissions = newPermissions.stream()
                .map(permission -> RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .build())
                .collect(Collectors.toList());

        rolePermissionRepository.saveAll(newRolePermissions);

        role.updatePermission(newRolePermissions);
        roleRepository.save(role);

        return role.getId();
    }

    // 역할 수정
    @CacheEvict(value = "permissions", allEntries = true)
    public void updateMemberRole(UUID adminMemberPositionId, UUID targetMemberPositionId, UpdateMemberRoleReq req) {
        if (checkPermission(adminMemberPositionId, "member", Action.UPDATE).equals(Bool.FALSE)) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        MemberPosition targetMemberPosition = memberPositionRepository.findById(targetMemberPositionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직책입니다."));

        Role newRole = roleRepository.findById(req.getNewRoleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 역할입니다."));

        targetMemberPosition.updateRole(newRole);
        memberPositionRepository.save(targetMemberPosition);
    }

    // member_position 생성
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
}