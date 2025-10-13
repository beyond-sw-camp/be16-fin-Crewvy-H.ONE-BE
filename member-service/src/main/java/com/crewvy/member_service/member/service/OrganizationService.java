package com.crewvy.member_service.member.service;

import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.constant.PermissionRange;
import com.crewvy.member_service.member.dto.request.CreateOrganizationReq;
import com.crewvy.member_service.member.dto.request.UpdateOrganizationReq;
import com.crewvy.member_service.member.dto.response.OrganizationTreeRes;
import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Member;
import com.crewvy.member_service.member.entity.Organization;
import com.crewvy.member_service.member.repository.CompanyRepository;
import com.crewvy.member_service.member.repository.MemberRepository;
import com.crewvy.member_service.member.repository.OrganizationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;

@Service
@Transactional
@Slf4j
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
        if (memberService.checkPermission(memberPositionId, "organization", Action.CREATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("조직을 생성할 권한이 없습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Organization parent = organizationRepository.findById(createOrganizationReq.getParentId()).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 상위 조직입니다."));

        if (!parent.getCompany().equals(member.getCompany())) {
            throw new IllegalArgumentException("상위 조직과 다른 회사에 속할 수 없습니다.");
        }

        int displayOrder = organizationRepository.findByParentIdOrderByDisplayOrderAsc(parent.getId()).size();
        Organization newOrganization = createOrganizationReq.toEntity(parent, member.getCompany());
        newOrganization.updateDisplayOrder(displayOrder);

        return organizationRepository.save(newOrganization).getId();
    }

    // 회원가입 시 사용되는 기본 조직 생성
    public Organization createDefaultOrganization(Company company) {
        Organization organization = Organization.builder()
                .parent(null)
                .name(company.getCompanyName())
                .company(company)
                .children(null)
                .displayOrder(0)
                .build();
        return organizationRepository.save(organization);
    }

    // 조직 목록 조회
    @Transactional(readOnly = true)
    public List<OrganizationTreeRes> getOrganizationList(UUID memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        Company company = member.getCompany();

        List<Organization> organizations = organizationRepository.findByCompanyOrderByDisplayOrderAsc(company);
        return organizations.stream()
                .filter(o -> o.getParent() == null)
                .map(OrganizationTreeRes::new)
                .collect(Collectors.toList());
    }

    // 조직 수정
    public UUID updateOrganization(UUID memberId, UUID memberPositionId, UUID organizationId, UpdateOrganizationReq updateOrganizationReq) {
        if (memberService.checkPermission(memberPositionId, "organization", Action.UPDATE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("조직을 수정할 권한이 없습니다.");
        }

        Organization organization = organizationRepository.findById(organizationId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 조직입니다."));

        organization.updateName(updateOrganizationReq.getName());
        return organizationRepository.save(organization).getId();
    }

    // 조직 삭제
    public void deleteOrganization(UUID memberId, UUID memberPositionId, UUID organizationId) {
        if (memberService.checkPermission(memberPositionId, "organization", Action.DELETE, PermissionRange.COMPANY) == FALSE) {
            throw new PermissionDeniedException("조직을 삭제할 권한이 없습니다.");
        }

        Organization organization = organizationRepository.findById(organizationId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 조직입니다."));

        // 하위 조직이 있는지 확인
        if (!organizationRepository.findByParentIdOrderByDisplayOrderAsc(organizationId).isEmpty()) {
            throw new IllegalArgumentException("하위 조직이 존재하여 삭제할 수 없습니다.");
        }

        // 해당 조직에 속한 멤버가 있는지 확인
        if (memberRepository.countByOrganizationId(organizationId) > 0) {
            throw new IllegalArgumentException("해당 조직에 속한 멤버가 존재하여 삭제할 수 없습니다.");
        }

        organizationRepository.delete(organization);
    }

    // 조직 순서 변경
    @Transactional
    public void reorderOrganization(List<UUID> organizationIds) {
        log.info("Reordering organizations: {}", organizationIds);
        List<Organization> organizationsToUpdate = new ArrayList<>();
        for (int i = 0; i < organizationIds.size(); i++) {
            int displayOrder = i;
            UUID id = organizationIds.get(i);
            organizationRepository.findById(id).ifPresent(organization -> {
                log.info("Updating organization {} with displayOrder {}", id, displayOrder);
                organization.updateDisplayOrder(displayOrder);
                organizationsToUpdate.add(organization);
            });
        }
        organizationRepository.saveAll(organizationsToUpdate);
    }
}
