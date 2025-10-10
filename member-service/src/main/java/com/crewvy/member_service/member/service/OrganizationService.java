package com.crewvy.member_service.member.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.dto.request.CreateOrganizationReq;
import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Member;
import com.crewvy.member_service.member.entity.Organization;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.member_service.member.repository.CompanyRepository;
import com.crewvy.member_service.member.repository.MemberRepository;
import com.crewvy.member_service.member.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               CompanyRepository companyRepository, MemberRepository memberRepository,
                               MemberService memberService) {
        this.organizationRepository = organizationRepository;
        this.companyRepository = companyRepository;
        this.memberRepository = memberRepository;
        this.memberService = memberService;
    }

    // 회사 생성
    public Company createCompany(String companyName, String businessNumber) {
        Company company = Company.builder()
                .companyName(companyName)
                .businessNumber(businessNumber)
                .build();
        return companyRepository.save(company);
    }

    // 조직 생성
    public UUID createOrganization(UUID memberId, UUID memberPositionId, CreateOrganizationReq createOrganizationReq) {
        if (memberService.checkPermission(memberPositionId, "organization", Action.CREATE).equals(Bool.FALSE)) {
            throw new PermissionDeniedException("조직을 생성할 권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Organization parent = organizationRepository.findById(createOrganizationReq.getParentId()).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 상위 조직입니다."));

        if (!parent.getCompany().equals(member.getCompany())) {
            throw new IllegalArgumentException("상위 조직과 다른 회사에 속할 수 없습니다.");
        }

        return organizationRepository.save(createOrganizationReq.toEntity(parent, member.getCompany())).getId();
    }

    // 회원가입 시 사용되는 기본 조직 생성
    public Organization createDefaultOrganization(Company company) {
        Organization organization = Organization.builder()
                .parent(null)
                .name(company.getCompanyName())
                .company(company)
                .children(null)
                .build();
        return organizationRepository.save(organization);
    }

    // 조직 목록 조회
    @Transactional(readOnly = true)
    public List<Organization> getOrganizationList(UUID memberId, UUID memberPositionId) {
        if (memberService.checkPermission(memberPositionId, "organization", Action.READ).equals(Bool.FALSE)) {
            throw new PermissionDeniedException("조직을 조회할 권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = member.getCompany();

        return organizationRepository.findByCompany(company);
    }
}
