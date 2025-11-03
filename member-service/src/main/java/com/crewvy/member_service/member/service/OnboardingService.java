package com.crewvy.member_service.member.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.dto.request.CreateAdminReq;
import com.crewvy.member_service.member.entity.*;
import com.crewvy.member_service.member.event.MemberChangedEvent;
import com.crewvy.member_service.member.repository.GradeHistoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class OnboardingService {

    private final MemberService memberService;
    private final OrganizationService organizationService;
    private final GradeHistoryRepository gradeHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OnboardingService(MemberService memberService, OrganizationService organizationService
            , GradeHistoryRepository gradeHistoryRepository, ApplicationEventPublisher eventPublisher) {
        this.memberService = memberService;
        this.organizationService = organizationService;
        this.gradeHistoryRepository = gradeHistoryRepository;
        this.eventPublisher = eventPublisher;
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
        eventPublisher.publishEvent(new MemberChangedEvent(adminMember.getId()));

        return adminMember.getId();
    }
}
