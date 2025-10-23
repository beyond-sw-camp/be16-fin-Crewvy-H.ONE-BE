package com.crewvy.workforce_service.attendance.config;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.PolicyCategory;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.dto.rule.AuthMethodDto;
import com.crewvy.workforce_service.attendance.dto.rule.AuthRuleDto;
import com.crewvy.workforce_service.attendance.dto.rule.PolicyRuleDetails;
import com.crewvy.workforce_service.attendance.dto.rule.WorkTimeRuleDto;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyType;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AttendanceTestDataInitializer implements CommandLineRunner {

    private final PolicyTypeRepository policyTypeRepository;
    private final PolicyRepository policyRepository;

    // member-service의 AutoCreateAdmin에서 생성되는 H.ONE 컴퍼니 ID
    private static final UUID COMPANY_ID = UUID.fromString("5568d353-2fa6-483d-8e79-dbd735dd71c2");

    @Override
    @Transactional
    public void run(String... args) {
        // H.ONE 컴퍼니에 대한 데이터만 초기화
        if (policyTypeRepository.countByCompanyId(COMPANY_ID) > 0) {
            return;
        }

        // 1. 정책 유형 (PolicyType) 생성
        // 참고: PolicyType에 priority 필드가 추가되었다고 가정합니다.
        Map<PolicyTypeCode, PolicyType> policyTypes = createPolicyTypes();

        // 2. 정책 (Policy) 생성
        createPolicies(policyTypes);
    }

    private Map<PolicyTypeCode, PolicyType> createPolicyTypes() {
        List<PolicyType> typesToCreate = List.of(
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.STANDARD_WORK).typeName("기본 근무").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(3).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.ANNUAL_LEAVE).typeName("연차").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.BUSINESS_TRIP).typeName("출장").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(2).build()
        );

        List<PolicyType> savedTypes = policyTypeRepository.saveAll(typesToCreate);
        return savedTypes.stream().collect(Collectors.toMap(PolicyType::getTypeCode, type -> type));
    }

    private void createPolicies(Map<PolicyTypeCode, PolicyType> policyTypes) {
        // --- [기본] 9-6 근무 정책 ---
        WorkTimeRuleDto workTimeRule = new WorkTimeRuleDto();
        workTimeRule.setType("FIXED");
        workTimeRule.setWorkStartTime("09:00");
        workTimeRule.setWorkEndTime("18:00");
        workTimeRule.setFixedWorkMinutes(480);

        AuthMethodDto webAuth = new AuthMethodDto();
        webAuth.setDeviceType(DeviceType.LAPTOP); // PC_WEB -> LAPTOP
        webAuth.setAuthMethod("NETWORK_IP");
        webAuth.setDetails(Map.of("allowedIps", List.of("127.0.0.1", "0:0:0:0:0:0:0:1")));

        AuthMethodDto mobileAuth = new AuthMethodDto();
        mobileAuth.setDeviceType(DeviceType.MOBILE); // MOBILE 사용
        mobileAuth.setAuthMethod("GPS");
        mobileAuth.setDetails(Map.of("officeLatitude", 37.5041, "officeLongitude", 127.0442, "gpsRadiusMeters", 300.0));
        
        AuthRuleDto authRule = new AuthRuleDto();
        authRule.setMethods(List.of(webAuth, mobileAuth));

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setWorkTimeRule(workTimeRule);
        ruleDetails.setAuthRule(authRule);

        Policy defaultPolicy = Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.STANDARD_WORK))
                .companyId(COMPANY_ID)
                .name("[기본] 9-6 고정 근무")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.now().withDayOfYear(1))
                .isActive(true)
                .build();

        // --- 2025년 연차 정책 ---
        Policy annualLeavePolicy = Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.ANNUAL_LEAVE))
                .companyId(COMPANY_ID)
                .name("2025년 연차 정책")
                .ruleDetails(new PolicyRuleDetails()) // 연차는 별도 규칙보다 타입 자체가 중요
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .effectiveTo(LocalDate.of(2025, 12, 31))
                .isActive(true)
                .build();

        policyRepository.saveAll(List.of(defaultPolicy, annualLeavePolicy));
    }
}
