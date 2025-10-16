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
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Service
@Transactional
public class MemberService {

    private final CompanyRepository companyRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final GradeRepository gradeRepository;
    private final GradeHistoryRepository gradeHistoryRepository;
    private final TitleRepository titleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public MemberService(CompanyRepository companyRepository, OrganizationRepository organizationRepository
            , MemberRepository memberRepository, MemberPositionRepository memberPositionRepository
            , RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository
            , GradeRepository gradeRepository, GradeHistoryRepository gradeHistoryRepository
            , TitleRepository titleRepository, PermissionRepository permissionRepository
            , PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.companyRepository = companyRepository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.memberPositionRepository = memberPositionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.gradeRepository = gradeRepository;
        this.gradeHistoryRepository = gradeHistoryRepository;
        this.titleRepository = titleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

    // 계정 생성
    public UUID createMember(UUID memberId, UUID memberPositionId, CreateMemberReq createMemberReq) {
        if (checkPermission(memberPositionId, "member", Action.CREATE, PermissionRange.COMPANY) == FALSE) {
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

        Grade grade = gradeRepository.findById(createMemberReq.getGradeId()).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 직급입니다."));

        GradeHistory gradeHistory = GradeHistory.builder()
                .member(savedMember)
                .grade(grade)
                .isActive(Bool.TRUE)
                .build();
        gradeHistoryRepository.save(gradeHistory);

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

    // 이메일 중복 확인
    @Transactional(readOnly = true)
    public boolean emailExist(String email) {
        return memberRepository.findByEmail(email).isPresent();
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

    // 직원 목록 조회
    @Transactional(readOnly = true)
    public List<MemberListRes> getMemberList(UUID memberId, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "member", Action.READ, PermissionRange.COMPANY) == FALSE &&
                checkPermission(memberPositionId, "member", Action.READ, PermissionRange.SYSTEM) == FALSE) {
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

    // 직원 정보 수정 페이지
    @Transactional(readOnly = true)
    public MemberEditRes getMemberEditPage(UUID memberPositionId, UUID memberId) {
        if (checkPermission(memberPositionId, "member", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findByIdWithDetail(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        MemberPosition requesterPosition = memberPositionRepository.findById(memberPositionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직책입니다."));
        Company company = requesterPosition.getMember().getCompany();

        List<Grade> allGrade = gradeRepository.findAllByCompany(company);
        List<Organization> allOrganization = organizationRepository.findAllByCompany(company);
        List<Title> allTitle = titleRepository.findAllByCompany(company);
        List<Role> allRole = roleRepository.findAllByCompanyAndYnDel(company, Bool.TRUE);

        return MemberEditRes.toEntity(member, allGrade, allOrganization, allTitle, allRole);
    }

    // 직원 정보 수정
    public UUID updateMember(UUID memberPositionId, UUID memberId, UpdateMemberReq updateMemberReq) {
        if (checkPermission(memberPositionId, "member", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        // 기본 정보 수정
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 계정입니다."));
        String encodePw = member.getPassword();

        if (updateMemberReq.getNewPw() != null && !updateMemberReq.getNewPw().isBlank()) {
            encodePw = passwordEncoder.encode(updateMemberReq.getNewPw());
        }

        member.updateBasicInfo(updateMemberReq, encodePw);

        Set<GradeHistory> currentGradeHistorySet = new LinkedHashSet<>(member.getGradeHistorySet());
        Set<UUID> newGradeHistoryIdSet = new LinkedHashSet<>();
        List<GradeHistory> processedGradeHistoryList = new ArrayList<>();

        if (updateMemberReq.getGradeHistoryReqList() != null) {
            for (GradeHistoryReq req : updateMemberReq.getGradeHistoryReqList()) {
                GradeHistory gradeHistory = null;
                if (req.getGradeHistoryId() != null) { // 기존 gradeHistory 업데이트
                    gradeHistory = currentGradeHistorySet.stream()
                            .filter(gh -> gh.getId().equals(req.getGradeHistoryId()))
                            .findFirst()
                            .orElse(null);
                    if (gradeHistory != null) {
                        newGradeHistoryIdSet.add(gradeHistory.getId());
                    }
                }

                if (gradeHistory == null) { // 새로운 gradeHistory 생성
                    gradeHistory = new GradeHistory();
                    gradeHistory.updateMember(member);
                    gradeHistory.updateIsActive(Bool.TRUE); // isActive 기본값 설정
                }

                Grade grade = null;
                if (req.getGradeId() != null) {
                    grade = gradeRepository.findById(req.getGradeId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직급입니다."));
                } else {
                    throw new IllegalArgumentException("직급 ID는 null일 수 없습니다.");
                }
                gradeHistory.update(grade, req.getPromotionDate()); // isActive 파라미터 제거
                gradeHistory.updateYnDel(Bool.FALSE); // ynDel 초기화
                gradeHistoryRepository.save(gradeHistory); // 변경된 gradeHistory 저장
                processedGradeHistoryList.add(gradeHistory);
            }
        }

        // 삭제 처리: 프론트엔드에서 넘어오지 않은 기존 gradeHistory는 ynDel = TRUE
        currentGradeHistorySet.stream()
                .filter(gh -> !newGradeHistoryIdSet.contains(gh.getId()))
                .forEach(gh -> {
                    gh.updateYnDel(Bool.FALSE);
                    gradeHistoryRepository.save(gh);
                });

        // isActive 설정 로직: promotionDate가 가장 최근인 얘의 isActive를 TRUE로, 나머지는 FALSE로
        processedGradeHistoryList.stream()
                .filter(gh -> gh.getYnDel() == Bool.FALSE) // 삭제되지 않은 항목만 대상으로
                .max(Comparator.comparing(GradeHistory::getPromotionDate))
                .ifPresent(latestGradeHistory -> {
                    processedGradeHistoryList.forEach(gh -> {
                        if (gh.getYnDel() == Bool.FALSE) { // 삭제되지 않은 항목만 isActive 설정
                            gh.updateIsActive(gh.getId().equals(latestGradeHistory.getId()) ? Bool.TRUE : Bool.FALSE);
                            gradeHistoryRepository.save(gh);
                        }
                    });
                });

        Set<MemberPosition> currentPositionSet = new LinkedHashSet<>(member.getMemberPositionList());
        Set<UUID> updatedMemberPositionIds = new LinkedHashSet<>();

        if (updateMemberReq.getPositionUpdateReqList() != null) {
            for (PositionUpdateReq req : updateMemberReq.getPositionUpdateReqList()) {
                MemberPosition memberPosition = null;

                if (req.getMemberPositionId() != null) { // 기존 memberPosition 업데이트
                    memberPosition = currentPositionSet.stream()
                            .filter(mp -> mp.getId().equals(req.getMemberPositionId()))
                            .findFirst()
                            .orElse(null);
                    if (memberPosition != null) {
                        updatedMemberPositionIds.add(memberPosition.getId());
                    }
                }

                if (memberPosition == null) { // 새로운 memberPosition 생성
                    memberPosition = new MemberPosition();
                    memberPosition.updateMember(member);
                }

                Organization organization = null;
                if (req.getOrganizationId() != null) {
                    organization = organizationRepository.findById(req.getOrganizationId())
                            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 조직입니다."));
                } else {
                    throw new IllegalArgumentException("조직 ID는 null일 수 없습니다.");
                }

                Title title = null;
                if (req.getTitleId() != null) {
                    title = titleRepository.findById(req.getTitleId())
                            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 직책입니다."));
                } else {
                    throw new IllegalArgumentException("직책 ID는 null일 수 없습니다.");
                }

                Role role = null;
                if (req.getRoleId() != null) {
                    role = roleRepository.findById(req.getRoleId())
                            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 역할입니다."));
                } else {
                    throw new IllegalArgumentException("역할 ID는 null일 수 없습니다.");
                }

                memberPosition.update(organization, title, role, req.getStartDate());
                memberPosition.delete();
                memberPositionRepository.save(memberPosition);
            }
        }

        // 삭제 처리: 프론트엔드에서 넘어오지 않은 기존 memberPosition은 ynDel = TRUE
        currentPositionSet.stream()
                .filter(mp -> !updatedMemberPositionIds.contains(mp.getId()))
                .forEach(mp -> {
                    mp.delete(); // ynDel = TRUE
                    memberPositionRepository.save(mp);
                });
        return member.getId();
    }

    // 직원 상세 조회
    @Transactional(readOnly = true)
    public MemberDetailRes getMemberDetail(UUID uuid, UUID memberPositionId, UUID memberId) {
        if (checkPermission(memberPositionId, "member", Action.READ, PermissionRange.DEPARTMENT) == FALSE
                && checkPermission(memberPositionId, "member", Action.READ, PermissionRange.COMPANY) == FALSE
                && checkPermission(memberPositionId, "member", Action.READ, PermissionRange.SYSTEM) == FALSE) {
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

    // 직책 생성
    public UUID createTitle(UUID memberId, UUID memberPositionId, CreateTitleReq createTitleReq) {
        if (checkPermission(memberPositionId, "title", Action.CREATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        return titleRepository.save(createTitleReq.toEntity(member.getCompany())).getId();
    }

    // 직책 목록 조회
    @Transactional(readOnly = true)
    public List<TitleRes> getTitle(UUID memberId, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "title", Action.READ, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = member.getCompany();
        return titleRepository.findAllByCompanyOrderByDisplayOrderAsc(company).stream() // displayOrder로 정렬
                .map(TitleRes::fromEntity)
                .collect(Collectors.toList());
    }

    // 직책 수정
    public void updateTitle(UUID memberPositionId, UUID titleId, UpdateTitleReq updateTitleReq) {
        if (checkPermission(memberPositionId, "title", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Title title = titleRepository.findById(titleId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 직책입니다."));

        title.updateName(updateTitleReq.getName());
        titleRepository.save(title);
    }

    // 직책 순서 변경
    public void reorderTitles(UUID memberPositionId, ReorderReq reorderReq) {
        if (checkPermission(memberPositionId, "title", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        MemberPosition requesterPosition = memberPositionRepository.findById(memberPositionId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 직책입니다."));
        Company company = requesterPosition.getMember().getCompany();

        List<Title> titles = titleRepository.findAllById(reorderReq.getIdList());
        Map<UUID, Title> titleMap = titles.stream().collect(Collectors.toMap(Title::getId, title -> title));

        for (int i = 0; i < reorderReq.getIdList().size(); i++) {
            UUID titleId = reorderReq.getIdList().get(i);
            Title title = titleMap.get(titleId);
            if (title != null) {
                if (title.getCompany().equals(company)) {
                    title.updateDisplayOrder(i);
                    titleRepository.save(title);
                } else {
                    throw new PermissionDeniedException("다른 회사의 직책을 수정할 권한이 없습니다.");
                }
            }
        }
    }

    // 직책 삭제
    public void deleteTitle(UUID memberPositionId, UUID titleId) {
        if (checkPermission(memberPositionId, "title", Action.DELETE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        titleRepository.deleteById(titleId);
    }

    // 직급 생성
    public UUID createGrade(UUID memberId, UUID memberPositionId, CreateGradeReq createGradeReq) {
        if (checkPermission(memberPositionId, "grade", Action.CREATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        return gradeRepository.save(createGradeReq.toEntity(member.getCompany())).getId();
    }

    // 직급 목록 조회
    @Transactional(readOnly = true)
    public List<GradeRes> getGrade(UUID memberId, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "grade", Action.READ, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = member.getCompany();
        return gradeRepository.findAllByCompanyOrderByDisplayOrderAsc(company).stream() // displayOrder로 정렬
                .map(GradeRes::fromEntity)
                .collect(Collectors.toList());
    }

    // 직급 수정
    public void updateGrade(UUID memberPositionId, UUID gradeId, UpdateGradeReq updateGradeReq) {
        if (checkPermission(memberPositionId, "grade", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Grade grade = gradeRepository.findById(gradeId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 직급입니다."));

        grade.updateName(updateGradeReq.getName());
        gradeRepository.save(grade);
    }

    // 직급 순서 변경
    public void reorderGrades(UUID memberPositionId, ReorderReq reorderReq) {
        if (checkPermission(memberPositionId, "grade", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        MemberPosition requesterPosition = memberPositionRepository.findById(memberPositionId)
                .orElseThrow(() -> {
                    return new EntityNotFoundException("존재하지 않는 직책입니다.");
                });
        Company company = requesterPosition.getMember().getCompany();

        List<Grade> grades = gradeRepository.findAllById(reorderReq.getIdList());
        Map<UUID, Grade> gradeMap = grades.stream().collect(Collectors.toMap(Grade::getId, grade -> grade));

        for (int i = 0; i < reorderReq.getIdList().size(); i++) {
            UUID gradeId = reorderReq.getIdList().get(i);
            Grade grade = gradeMap.get(gradeId);
            if (grade != null) {
                if (grade.getCompany().equals(company)) {
                    grade.updateDisplayOrder(i);
                    gradeRepository.save(grade);
                } else {
                    throw new PermissionDeniedException("다른 회사의 직급을 수정할 권한이 없습니다。");
                }
            }
        }
    }

    // 직급 삭제
    public void deleteGrade(UUID memberPositionId, UUID gradeId) {
        if (checkPermission(memberPositionId, "grade", Action.DELETE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        gradeRepository.deleteById(gradeId);
    }

    // 역할 생성
    @CacheEvict(cacheManager = "permissionCacheManager", value = "permissions", allEntries = true)
    public UUID createRole(UUID memberPositionId, RoleUpdateReq request) {
        if (checkPermission(memberPositionId, "role", Action.CREATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        MemberPosition creatorPosition = memberPositionRepository.findById(memberPositionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직책입니다."));
        Company company = creatorPosition.getMember().getCompany();

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .company(company)
                .build();

        roleRepository.save(role); // Save the Role first

        List<RolePermission> newRolePermissions = request.getPermissions().stream()
                .map(pReq -> {
                    Permission permission = permissionRepository.findById(pReq.getPermissionId())
                            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 권한입니다. ID: " + pReq.getPermissionId()));

                    PermissionRange selectedRange = PermissionRange.valueOf(pReq.getSelectedRange());

                    return RolePermission.builder()
                            .role(role)
                            .permission(permission)
                            .permissionRange(selectedRange)
                            .build();
                })
                .collect(Collectors.toList());

        rolePermissionRepository.saveAll(newRolePermissions);

        role.updatePermission(newRolePermissions);
        return roleRepository.save(role).getId();
    }

    // 역할 목록 조회
    @Transactional(readOnly = true)
    public List<RoleRes> getRole(UUID memberId, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "role", Action.READ, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = member.getCompany();
        List<Role> roles = roleRepository.findAllByCompanyAndYnDelOrderByDisplayOrderAsc(company, Bool.TRUE);

        return roles.stream().map(role -> {
            List<MemberPosition> memberPositions = memberPositionRepository.findByRole(role);
            List<RoleMemberRes> memberList = memberPositions.stream()
                    .map(RoleMemberRes::fromEntity)
                    .collect(Collectors.toList());
            return RoleRes.fromEntity(role, memberList);
        }).collect(Collectors.toList());
    }

    // 역할 상세 조회
    @Transactional(readOnly = true)
    public RoleDetailRes getRoleById(UUID memberPositionId, UUID roleId) {
        if (checkPermission(memberPositionId, "role", Action.READ, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 역할입니다."));

        List<Permission> allPermissions = permissionRepository.findAllByPermissionRangeNot(PermissionRange.SYSTEM);

        Map<String, PermissionRange> rolePermissionMap = role.getRolePermissionList().stream()
                .collect(Collectors.toMap(
                        rp -> rp.getPermission().getResource() + ":" + rp.getPermission().getAction(),
                        RolePermission::getPermissionRange,
                        (existing, replacement) -> existing.ordinal() > replacement.ordinal() ? existing : replacement
                ));

        Map<String, List<Permission>> groupedPermissions = allPermissions.stream()
                .collect(Collectors.groupingBy(p -> p.getResource() + ":" + p.getAction()));

        List<PermissionRes> permissionResList = groupedPermissions.entrySet().stream()
                .map(entry -> {
                    String groupKey = entry.getKey();
                    List<Permission> permsInGroup = entry.getValue();
                    Permission permission = permsInGroup.get(0);

                    PermissionRange permissionRange = rolePermissionMap.getOrDefault(groupKey, PermissionRange.NONE);

                    Map<PermissionRange, UUID> rangeToIdMap = permsInGroup.stream()
                            .collect(Collectors.toMap(Permission::getPermissionRange, Permission::getId));

                    return PermissionRes.fromEntity(permission, permissionRange, rangeToIdMap);
                })
                .collect(Collectors.toList());

        return RoleDetailRes.fromEntity(role, permissionResList);
    }

    // 역할 수정
    @CacheEvict(cacheManager = "permissionCacheManager", value = "permissions", allEntries = true)
    public UUID updateRole(UUID memberPositionId, UUID roleId, RoleUpdateReq request) {
        if (checkPermission(memberPositionId, "role", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 역할입니다."));

        role.updateDescription(request.getDescription());
        role.updateName(request.getName());

        List<RolePermission> newRolePermissions = request.getPermissions().stream()
                .map(pReq -> {
                    Permission permission = permissionRepository.findById(pReq.getPermissionId())
                            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 권한입니다. ID: " + pReq.getPermissionId()));

                    PermissionRange selectedRange = PermissionRange.valueOf(pReq.getSelectedRange());

                    return RolePermission.builder()
                            .role(role)
                            .permission(permission)
                            .permissionRange(selectedRange)
                            .build();
                })
                .collect(Collectors.toList());

        rolePermissionRepository.saveAll(newRolePermissions);

        role.updatePermission(newRolePermissions);
        return roleRepository.save(role).getId();
    }

    // 멤버의 역할 변경
    @CacheEvict(cacheManager = "permissionCacheManager", value = "permissions", allEntries = true)
    public void updateMemberRole(UUID adminMemberPositionId, UUID targetMemberPositionId, UpdateMemberRoleReq req) {
        if (checkPermission(adminMemberPositionId, "role", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        MemberPosition targetMemberPosition = memberPositionRepository.findById(targetMemberPositionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직책입니다."));

        Role newRole = roleRepository.findById(req.getNewRoleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 역할입니다."));

        targetMemberPosition.updateRole(newRole);
        memberPositionRepository.save(targetMemberPosition);
    }

    // 역할 순서 변경
    public void reorderRoles(UUID memberPositionId, ReorderReq reorderReq) {
        if (checkPermission(memberPositionId, "role", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        MemberPosition requesterPosition = memberPositionRepository.findById(memberPositionId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 직책입니다."));
        Company company = requesterPosition.getMember().getCompany();

        List<Role> roles = roleRepository.findAllById(reorderReq.getIdList());
        Map<UUID, Role> roleMap = roles.stream().collect(Collectors.toMap(Role::getId, role -> role));

        for (int i = 0; i < reorderReq.getIdList().size(); i++) {
            UUID roleId = reorderReq.getIdList().get(i);
            Role role = roleMap.get(roleId);
            if (role != null) {
                if (role.getCompany().equals(company)) {
                    role.updateDisplayOrder(i);
                    roleRepository.save(role);
                } else {
                    throw new PermissionDeniedException("다른 회사의 역할을 수정할 권한이 없습니다.");
                }
            }
        }
    }

    // 역할 삭제
    public void deleteRole(UUID memberPositionId, UUID roleId) {
        if (checkPermission(memberPositionId, "role", Action.DELETE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 역할입니다."));
        role.delete();
        roleRepository.save(role);
    }

    // 마이페이지
    public MyPageRes myPage(UUID uuid, UUID memberPositionId) {
        if (checkPermission(memberPositionId, "member", Action.READ, PermissionRange.INDIVIDUAL) == FALSE &&
                checkPermission(memberPositionId, "member", Action.READ, PermissionRange.DEPARTMENT) == FALSE &&
                checkPermission(memberPositionId, "member", Action.READ, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(uuid).orElseThrow(()
                -> new EntityNotFoundException("존재하지 않는 계정입니다."));
        MemberPosition memberPosition = memberPositionRepository.findById(memberPositionId).orElseThrow(()
                -> new EntityNotFoundException("존재하지 않는 memberPosition입니다."));
        GradeHistory gradeHistory = gradeHistoryRepository.findByMemberAndIsActive(member, Bool.TRUE).orElseThrow(()
                -> new EntityNotFoundException("사용중인 직급이 없습니다."));
        Grade grade = gradeRepository.findById(gradeHistory.getGrade().getId()).orElseThrow(()
                -> new EntityNotFoundException("존재하지 않는 직급입니다."));
        return MyPageRes.fromEntity(member, memberPosition, grade);
    }

    // 마이페이지 수정
    public void updateMyPage(UUID memberId, UUID memberPositionId, MyPageEditReq myPageEditReq) {
        if (checkPermission(memberPositionId, "member", Action.UPDATE, PermissionRange.INDIVIDUAL) == FALSE &&
                checkPermission(memberPositionId, "member", Action.UPDATE, PermissionRange.DEPARTMENT) == FALSE &&
                checkPermission(memberPositionId, "member", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 계정입니다."));

        String encodePw = member.getPassword();
        String currentPassword = myPageEditReq.getCurrentPassword();
        String newPassword = myPageEditReq.getNewPassword();
        String confirmPassword = myPageEditReq.getConfirmPassword();

        if (currentPassword != null && !currentPassword.isBlank()) {
            if (!passwordEncoder.matches(currentPassword, encodePw)) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("새로운 비밀번호가 일치하지 않습니다.");
            }

            if (newPassword.length() < 8) {
                throw new IllegalArgumentException("새로운 비밀번호가 너무 짧습니다.");
            }

            if (passwordEncoder.matches(newPassword, encodePw)) {
                throw new IllegalArgumentException("기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
            }

            encodePw = passwordEncoder.encode(newPassword);
        }

        member.updateMyPage(myPageEditReq, encodePw);
    }

    // 권한 확인
    @Cacheable(cacheManager = "permissionCacheManager", value = "permissions", key = "#memberPositionId.toString() + ':' + #resource + ':' + #action.name() + ':' + #range.name()")
    @Transactional(readOnly = true)
    public Boolean checkPermission(UUID memberPositionId, String resource, Action action, PermissionRange range) {
        if (!permissionRepository.hasPermission(memberPositionId, resource, action, range)) {
            return FALSE;
        } else {
            return TRUE;
        }
    }

    // 모든 권한 목록 조회
    @Transactional(readOnly = true)
    public List<PermissionRes> getAllPermission() {
        List<Permission> permissionList = permissionRepository.findAllByPermissionRangeNot(PermissionRange.SYSTEM);

        Map<String, List<Permission>> groupedPermissionList = permissionList.stream()
                .collect(Collectors.groupingBy(p -> p.getResource() + ":" + p.getAction()));

        return groupedPermissionList.entrySet().stream()
                .map(entry -> {
                    List<Permission> permsInGroup = entry.getValue();
                    Permission permission = permsInGroup.get(0);

                    Map<PermissionRange, UUID> rangeToIdMap = permsInGroup.stream()
                            .collect(Collectors.toMap(Permission::getPermissionRange, Permission::getId));

                    return PermissionRes.fromEntity(permission, PermissionRange.NONE, rangeToIdMap);
                })
                .collect(Collectors.toList());
    }

    // 권한 확인 (캐시 없음)
    @Transactional(readOnly = true)
    public Boolean checkPermissionWithoutCache(UUID memberPositionId, String resource, Action action, PermissionRange range) {
        if (!permissionRepository.hasPermission(memberPositionId, resource, action, range)) {
            return FALSE;
        } else {
            return TRUE;
        }
    }

    private PermissionRange getHighestPermissionRangeForRead(UUID memberPositionId) {
        if (checkPermission(memberPositionId, "member", Action.READ, PermissionRange.COMPANY) == TRUE) {
            return PermissionRange.COMPANY;
        }
        if (checkPermission(memberPositionId, "member", Action.READ, PermissionRange.DEPARTMENT) == TRUE) {
            return PermissionRange.DEPARTMENT;
        }
        return null;
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

        List<String> resources = Arrays.asList("member", "title", "grade", "role", "organization", "attendance", "salary");
        List<Action> actions = Arrays.asList(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE);

        List<RolePermission> permissions = resources.stream()
                .flatMap(resource -> actions.stream().map(action -> {
                    Permission permission = permissionRepository.findByResourceAndActionAndPermissionRange(resource, action, PermissionRange.COMPANY)
                            .orElseThrow(() -> new IllegalStateException("존재하지 않는 권한입니다. (자원: " + resource + ", 작업: " + action + ")"));
                    return RolePermission.builder()
                            .role(adminRole)
                            .permission(permission)
                            .permissionRange(PermissionRange.COMPANY)
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

    // memberIdList → 이름 List
    @Transactional(readOnly = true)
    public List<MemberNameListRes> getNameList(UUID memberPositionId, IdListReq idListReq) {
        return memberRepository.findAllById(idListReq.getUuidList()).stream()
                .map(MemberNameListRes::fromEntity).collect(Collectors.toList());
    }

    // 조멤직IdList → ( 이름, 부서, 직급 ) List
    @Transactional(readOnly = true)
    public List<MemberPositionListRes> getPositionList(UUID memberPositionId, IdListReq idListReq) {
        return memberPositionRepository.findAllById(idListReq.getUuidList()).stream()
                .map(MemberPositionListRes::fromEntity).collect(Collectors.toList());
    }

    // companyId → ( 사번, 이름, 부서, 직급, 계좌, 은행 ) List
    @Transactional(readOnly = true)
    public List<MemberSalaryListRes> getSalaryList(UUID memberPositionId, UUID companyId) {
        if (checkPermission(memberPositionId, "salary", Action.READ, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("권한이 없습니다.");
        }
        if (!memberRepository.findById(memberPositionId).orElseThrow(() ->
                new EntityNotFoundException("존재하지 않는 계정입니다.")).getCompany().getId().equals(companyId)) {
            throw new PermissionDeniedException("다른 회사의 정보는 조회할 수 없습니다.");
        }
        return memberRepository.findByCompanyWithDetail(companyRepository.findById(companyId).orElseThrow(() ->
                        new EntityNotFoundException("존재하지 않는 회사입니다."))).stream()
                .map(mp -> MemberSalaryListRes.fromEntity(mp.getDefaultMemberPosition())).collect(Collectors.toList());
    }
}
