package com.crewvy.member_service.member.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.member_service.member.auth.JwtTokenProvider;
import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.constant.PermissionRange;
import com.crewvy.member_service.member.dto.request.*;
import com.crewvy.member_service.member.dto.response.*;
import com.crewvy.member_service.member.entity.*;
import com.crewvy.member_service.member.repository.*;
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
    private final OrganizationRepository organizationRepository;

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
        String refreshToken = jwtTokenProvider.createRtToken(member);

        return LoginRes.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userName(member.getName())
                .memberId(member.getId())
                .memberPositionId(member.getDefaultMemberPosition().getId())
                .build();
    }

    // 직책 생성
    public UUID createTitle(UUID memberId, UUID memberPositionId, CreateTitleReq createTitleReq) {
        if (checkPermission(memberPositionId, "title", Action.CREATE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        return titleRepository.save(createTitleReq.toEntity(member.getCompany())).getId();
    }

    // 직책 목록 조회
    @Transactional(readOnly = true)
    public List<TitleRes> getTitle(UUID memberId, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "title", Action.READ, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = member.getCompany();
        return titleRepository.findAllByCompany(company).stream()
                .map(TitleRes::fromEntity)
                .collect(Collectors.toList());
    }

    // 직책 수정
    public void updateTitle(UUID memberPositionId, UUID titleId, UpdateTitleReq updateTitleReq) {
        if (checkPermission(memberPositionId, "title", Action.UPDATE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Title title = titleRepository.findById(titleId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 직책입니다."));

        title.updateName(updateTitleReq.getName());
        titleRepository.save(title);
    }

    // 직책 삭제
    public void deleteTitle(UUID memberPositionId, UUID titleId) {
        if (checkPermission(memberPositionId, "title", Action.DELETE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        titleRepository.deleteById(titleId);
    }

    // 직급 생성
    public UUID createGrade(UUID memberId, UUID memberPositionId, CreateGradeReq createGradeReq) {
        if (checkPermission(memberPositionId, "grade", Action.CREATE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        return gradeRepository.save(createGradeReq.toEntity(member.getCompany())).getId();
    }

    // 직급 목록 조회
    @Transactional(readOnly = true)
    public List<GradeRes> getGrade(UUID memberId, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "grade", Action.READ, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = member.getCompany();
        return gradeRepository.findAllByCompany(company).stream()
                .map(GradeRes::fromEntity)
                .collect(Collectors.toList());
    }

    // 직급 수정
    public void updateGrade(UUID memberPositionId, UUID gradeId, UpdateGradeReq updateGradeReq) {
        if (checkPermission(memberPositionId, "grade", Action.UPDATE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Grade grade = gradeRepository.findById(gradeId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 직급입니다."));

        grade.updateName(updateGradeReq.getName());
        gradeRepository.save(grade);
    }

    // 직급 삭제
    public void deleteGrade(UUID memberPositionId, UUID gradeId) {
        if (checkPermission(memberPositionId, "grade", Action.DELETE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        gradeRepository.deleteById(gradeId);
    }

    // 역할 삭제
    public void deleteRole(UUID memberPositionId, UUID roleId) {
        if (checkPermission(memberPositionId, "role", Action.DELETE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 역할입니다."));
        role.delete();
        roleRepository.save(role);
    }

    // 역할 목록 조회
    @Transactional(readOnly = true)
    public List<RoleRes> getRole(UUID memberId, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "role", Action.READ, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = member.getCompany();
        List<Role> roles = roleRepository.findAllByCompanyAndYnDel(company, Bool.TRUE);

        return roles.stream().map(role -> {
            List<MemberPosition> memberPositions = memberPositionRepository.findByRole(role);
            List<RoleMemberRes> memberList = memberPositions.stream()
                    .map(RoleMemberRes::fromEntity)
                    .collect(Collectors.toList());
            return RoleRes.fromEntity(role, memberList);
        }).collect(Collectors.toList());
    }

    // 직원 목록 조회
    @Transactional(readOnly = true)
    public List<MemberListRes> getMemberList(UUID memberId, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "member", Action.READ, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue()) &&
                checkPermission(memberPositionId, "member", Action.READ, PermissionRange.SYSTEM).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        PermissionRange permissionRange = getHighestPermissionRangeForRead(memberPositionId);

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        if (permissionRange == PermissionRange.COMPANY) {
            Company company = member.getCompany();
            List<Member> memberList = memberRepository.findByCompanyWithDetail(company);
            return memberList.stream()
                    .map(Member::getDefaultMemberPosition)
                    .filter(java.util.Objects::nonNull)
                    .map(MemberListRes::fromEntity)
                    .collect(Collectors.toList());
        } else if (permissionRange == PermissionRange.DEPARTMENT) {
            MemberPosition memberPosition = memberPositionRepository.findById(memberPositionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직책입니다."));
            Organization organization = memberPosition.getOrganization();
            List<MemberPosition> memberPositionList = memberPositionRepository.findAllByOrganization(organization);
            return memberPositionList.stream()
                    .map(MemberPosition::getMember)
                    .distinct()
                    .map(Member::getDefaultMemberPosition)
                    .filter(java.util.Objects::nonNull)
                    .map(MemberListRes::fromEntity)
                    .collect(Collectors.toList());
        } else {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
    }

    // 직원 상세 조회
    @Transactional(readOnly = true)
    public MemberDetailRes getMemberDetail(UUID uuid, UUID memberPositionId, UUID memberId) {
        if (checkPermission(memberPositionId, "member", Action.READ, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue()) &&
                checkPermission(memberPositionId, "member", Action.READ, PermissionRange.SYSTEM).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        PermissionRange permissionRange = getHighestPermissionRangeForRead(memberPositionId);

        if (permissionRange == null) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member targetMember = memberRepository.findByIdWithDetail(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        if (permissionRange == PermissionRange.DEPARTMENT) {
            MemberPosition requestingPosition = memberPositionRepository.findById(memberPositionId)
                    .orElseThrow(() -> new IllegalArgumentException("직책 정보를 찾을 수 없습니다."));
            Organization requestingDepartment = requestingPosition.getOrganization();

            boolean isInSameDepartment = targetMember.getMemberPositionList().stream()
                    .anyMatch(pos -> pos.getOrganization().equals(requestingDepartment));

            if (!isInSameDepartment) {
                throw new PermissionDeniedException("다른 부서의 직원을 조회할 수 없습니다.");
            }
        }

        return MemberDetailRes.fromEntity(targetMember);
    }

    // 역할별 권한 수정
    @CacheEvict(cacheManager = "permissionCacheManager", value = "permissions", allEntries = true)
    public UUID updateRolePermission(UUID memberPositionId, UUID roleId, UpdateRolePermissionsReq req) {
        if (checkPermission(memberPositionId, "role", Action.UPDATE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 역할입니다."));

        rolePermissionRepository.deleteAllByRole(role);

        List<Permission> newPermissionList = permissionRepository.findAllById(req.getPermissionIds());

        List<RolePermission> newRolePermissionList = newPermissionList.stream()
                .map(permission -> RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .build())
                .collect(Collectors.toList());

        rolePermissionRepository.saveAll(newRolePermissionList);

        role.updatePermission(newRolePermissionList);
        roleRepository.save(role);

        return role.getId();
    }

    // 역할 수정
    @CacheEvict(cacheManager = "permissionCacheManager", value = "permissions", allEntries = true)
    public void updateMemberRole(UUID adminMemberPositionId, UUID targetMemberPositionId, UpdateMemberRoleReq req) {
        if (checkPermission(adminMemberPositionId, "role", Action.UPDATE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        MemberPosition targetMemberPosition = memberPositionRepository.findById(targetMemberPositionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직책입니다."));

        Role newRole = roleRepository.findById(req.getNewRoleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 역할입니다."));

        targetMemberPosition.updateRole(newRole);
        memberPositionRepository.save(targetMemberPosition);
    }

    // 권한 확인
    @Cacheable(cacheManager = "permissionCacheManager", value = "permissions", key = "#memberPositionId.toString() + ':' + #resource + ':' + #action.name() + ':' + #range.name()")
    @Transactional(readOnly = true)
    public String checkPermission(UUID memberPositionId, String resource, Action action, PermissionRange range) {
        if (!permissionRepository.hasPermission(memberPositionId, resource, action, range)) {
            return Bool.FALSE.getCodeValue();
        } else {
            return Bool.TRUE.getCodeValue();
        }
    }

    // 권한 확인 (캐시 없음)
    @Transactional(readOnly = true)
    public String checkPermissionWithoutCache(UUID memberPositionId, String resource, Action action, PermissionRange range) {
        if (!permissionRepository.hasPermission(memberPositionId, resource, action, range)) {
            return Bool.FALSE.getCodeValue();
        } else {
            return Bool.TRUE.getCodeValue();
        }
    }

    private PermissionRange getHighestPermissionRangeForRead(UUID memberPositionId) {
        if (checkPermission(memberPositionId, "member", Action.READ, PermissionRange.COMPANY).equals(Bool.TRUE.getCodeValue())) {
            return PermissionRange.COMPANY;
        }
        if (checkPermission(memberPositionId, "member", Action.READ, PermissionRange.DEPARTMENT).equals(Bool.TRUE.getCodeValue())) {
            return PermissionRange.DEPARTMENT;
        }
        return null;
    }

    // 계정 존재여부 확인
    @Transactional(readOnly = true)
    public boolean emailExist(String email) {
        return memberRepository.findByEmail(email).isPresent();
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

    // member_position 생성(AutoCreate)
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

        List<String> resources = Arrays.asList("member", "title", "grade", "role", "organization", "attendance", "payroll");
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
        if (checkPermission(memberPositionId, "member", Action.CREATE, PermissionRange.COMPANY).equals(Bool.FALSE.getCodeValue())) {
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

}