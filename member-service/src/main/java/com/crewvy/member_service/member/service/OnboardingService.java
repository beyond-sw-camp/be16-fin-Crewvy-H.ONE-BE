package com.crewvy.member_service.member.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.dto.request.CreateAdminReq;
import com.crewvy.member_service.member.entity.*;
import com.crewvy.member_service.member.repository.GradeHistoryRepository;
import com.crewvy.member_service.member.repository.SearchOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class OnboardingService {

    private final MemberService memberService;
    private final OrganizationService organizationService;
    private final GradeHistoryRepository gradeHistoryRepository;

    public OnboardingService(MemberService memberService, OrganizationService organizationService
            , GradeHistoryRepository gradeHistoryRepository) {
        this.memberService = memberService;
        this.organizationService = organizationService;
        this.gradeHistoryRepository = gradeHistoryRepository;
    }


    // 관리자 계정 생성과 회사, 조직, 역할 등 초기 설정
    public UUID createAdminAndInitialSetup(CreateAdminReq createAdminReq) {
        Company company = organizationService.createCompany(createAdminReq.getCompanyName(), createAdminReq.getBusinessNumber());

        Organization organization = organizationService.createDefaultOrganization(company);
        Role adminRole = memberService.createAdminRole(company);
        memberService.createBaseRole(company);
        Grade grade = memberService.createDefaultGrade(company);
        Title adminTitle = memberService.createDefaultTitle(company);

        Member adminMember = memberService.createAdminMember(createAdminReq, company);

        GradeHistory gradeHistory = GradeHistory.builder()
                .member(adminMember)
                .grade(grade)
                .isActive(Bool.TRUE)
                .build();
        gradeHistoryRepository.save(gradeHistory);

        memberService.createAndAssignDefaultPosition(null, adminMember, organization, adminTitle, adminRole);

        // 통합검색 kafka
        memberService.saveSearchOutboxEvent(adminMember.getId());

        return adminMember.getId();
    }
}
