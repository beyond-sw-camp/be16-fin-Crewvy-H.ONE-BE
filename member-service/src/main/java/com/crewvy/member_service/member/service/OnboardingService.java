package com.crewvy.member_service.member.service;

import com.crewvy.member_service.member.dto.request.CreateAdminReq;
import com.crewvy.member_service.member.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class OnboardingService {

    private final MemberService memberService;
    private final OrganizationService organizationService;

    public OnboardingService(MemberService memberService, OrganizationService organizationService) {
        this.memberService = memberService;
        this.organizationService = organizationService;
    }

     // 관리자 계정 생성과 회사, 조직, 역할 등 초기 설정
    public UUID createAdminAndInitialSetup(CreateAdminReq createAdminReq) {
        Company company = organizationService.createCompany(createAdminReq.getCompanyName(), createAdminReq.getBusinessNumber());

        Organization organization = organizationService.createDefaultOrganization(company);
        Role adminRole = memberService.createAdminRole(company);
        memberService.createBaseRole(company);
        memberService.createDefaultGrade(company);
        Title adminTitle = memberService.createDefaultTitle(company);

        Member adminMember = memberService.createAdminMember(createAdminReq, company);

        memberService.createAndAssignDefaultPosition(adminMember, organization, adminTitle, adminRole);

        return adminMember.getId();
    }
}
