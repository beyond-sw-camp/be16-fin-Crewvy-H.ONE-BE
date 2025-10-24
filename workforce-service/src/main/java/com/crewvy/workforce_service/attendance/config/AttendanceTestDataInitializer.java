package com.crewvy.workforce_service.attendance.config;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.PolicyCategory;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyType;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private static final UUID COMPANY_ID = UUID.fromString("14720b70-bfe3-4135-992a-e5f992338172");

    @Override
    @Transactional
    public void run(String... args) {
        // H.ONE 컴퍼니에 대한 데이터만 초기화
        if (policyTypeRepository.countByCompanyId(COMPANY_ID) > 0) {
            return;
        }

        // 1. 정책 유형 (PolicyType) 생성
        Map<PolicyTypeCode, PolicyType> policyTypes = createPolicyTypes();

        // 2. 정책 (Policy) 생성
        createPolicies(policyTypes);
    }

    private Map<PolicyTypeCode, PolicyType> createPolicyTypes() {
        List<PolicyType> typesToCreate = List.of(
                // 법정 휴가 (Priority 1)
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.ANNUAL_LEAVE).typeName("연차유급휴가").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.MATERNITY_LEAVE).typeName("출산전후휴가").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.PATERNITY_LEAVE).typeName("배우자 출산휴가").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.CHILDCARE_LEAVE).typeName("육아휴직").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.FAMILY_CARE_LEAVE).typeName("가족돌봄휴가").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.MENSTRUAL_LEAVE).typeName("생리휴가").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),

                // 근무 유형 (Priority 2, 3)
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.BUSINESS_TRIP).typeName("출장").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(2).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.STANDARD_WORK).typeName("기본근무").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(3).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.OVERTIME).typeName("연장근무").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(3).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.NIGHT_WORK).typeName("야간근무").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(3).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.HOLIDAY_WORK).typeName("휴일근무").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(3).build()
        );

        List<PolicyType> savedTypes = policyTypeRepository.saveAll(typesToCreate);
        return savedTypes.stream().collect(Collectors.toMap(PolicyType::getTypeCode, type -> type));
    }

    private void createPolicies(Map<PolicyTypeCode, PolicyType> policyTypes) {
        List<Policy> policies = new ArrayList<>();

        // ========== 1. 기본 근무 (STANDARD_WORK) ==========
        policies.add(createStandardWorkPolicy(policyTypes));

        // ========== 2. 연차유급휴가 (ANNUAL_LEAVE) ==========
        policies.add(createAnnualLeavePolicy(policyTypes));

        // ========== 3. 출산전후휴가 (MATERNITY_LEAVE) ==========
        policies.add(createMaternityLeavePolicy(policyTypes));

        // ========== 4. 배우자 출산휴가 (PATERNITY_LEAVE) ==========
        policies.add(createPaternityLeavePolicy(policyTypes));

        // ========== 5. 육아휴직 (CHILDCARE_LEAVE) ==========
        policies.add(createChildcareLeavePolicy(policyTypes));

        // ========== 6. 가족돌봄휴가 (FAMILY_CARE_LEAVE) ==========
        policies.add(createFamilyCareLeavePolicy(policyTypes));

        // ========== 7. 생리휴가 (MENSTRUAL_LEAVE) ==========
        policies.add(createMenstrualLeavePolicy(policyTypes));

        // ========== 8. 출장 (BUSINESS_TRIP) ==========
        policies.add(createBusinessTripPolicy(policyTypes));

        // ========== 9. 연장근무 (OVERTIME) ==========
        policies.add(createOvertimePolicy(policyTypes));

        // ========== 10. 야간근무 (NIGHT_WORK) ==========
        policies.add(createNightWorkPolicy(policyTypes));

        // ========== 11. 휴일근무 (HOLIDAY_WORK) ==========
        policies.add(createHolidayWorkPolicy(policyTypes));

        policyRepository.saveAll(policies);
    }

    // ========== 정책 생성 헬퍼 메서드 ==========

    private Policy createStandardWorkPolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        WorkTimeRuleDto workTimeRule = new WorkTimeRuleDto();
        workTimeRule.setType("FIXED");
        workTimeRule.setWorkStartTime("09:00");
        workTimeRule.setWorkEndTime("18:00");
        workTimeRule.setFixedWorkMinutes(480);

        BreakRuleDto breakRule = new BreakRuleDto();
        breakRule.setMandatoryBreakMinutes(60); // 8시간 근무, 60분 휴게 (법정)

        OvertimeRuleDto overtimeRule = new OvertimeRuleDto();
        overtimeRule.setAllowOvertime(true);
        overtimeRule.setMaxWeeklyOvertimeMinutes(720); // 주 12시간 한도
        overtimeRule.setOvertimeRate(new BigDecimal("1.5"));
        overtimeRule.setNightWorkRate(new BigDecimal("1.5"));
        overtimeRule.setHolidayWorkRate(new BigDecimal("1.5"));
        overtimeRule.setHolidayOvertimeRate(new BigDecimal("2.0"));

        AuthMethodDto webAuth = new AuthMethodDto();
        webAuth.setDeviceType(DeviceType.LAPTOP);
        webAuth.setAuthMethod("NETWORK_IP");
        webAuth.setDetails(Map.of("allowedIps", List.of("127.0.0.1", "0:0:0:0:0:0:0:1")));

        AuthMethodDto mobileAuth = new AuthMethodDto();
        mobileAuth.setDeviceType(DeviceType.MOBILE);
        mobileAuth.setAuthMethod("GPS");
        mobileAuth.setDetails(Map.of("officeLatitude", 37.5041, "officeLongitude", 127.0442, "gpsRadiusMeters", 300.0));

        AuthRuleDto authRule = new AuthRuleDto();
        authRule.setMethods(List.of(webAuth, mobileAuth));

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setWorkTimeRule(workTimeRule);
        ruleDetails.setBreakRule(breakRule);
        ruleDetails.setOvertimeRule(overtimeRule);
        ruleDetails.setAuthRule(authRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.STANDARD_WORK))
                .companyId(COMPANY_ID)
                .name("[기본] 9-6 고정 근무")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }

    private Policy createAnnualLeavePolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(15.0); // 법정 최소 15일
        leaveRule.setFirstYearMaxAccrual(11); // 1년 미만 근로자 최대 11일
        leaveRule.setAccrualType("AUTO");
        leaveRule.setMinimumRequestUnit("DAY");

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.ANNUAL_LEAVE))
                .companyId(COMPANY_ID)
                .name("2025년 연차 정책")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .effectiveTo(LocalDate.of(2025, 12, 31))
                .isActive(true)
                .build();
    }

    private Policy createMaternityLeavePolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(90.0); // 근로기준법 제74조 - 90일 (다태아 120일)
        leaveRule.setAccrualType("MANUAL");
        leaveRule.setMinimumRequestUnit("DAY");

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.MATERNITY_LEAVE))
                .companyId(COMPANY_ID)
                .name("출산전후휴가 정책")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }

    private Policy createPaternityLeavePolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(10.0); // 남녀고용평등법 제18조의2 - 10일
        leaveRule.setAccrualType("MANUAL");
        leaveRule.setMinimumRequestUnit("DAY");

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.PATERNITY_LEAVE))
                .companyId(COMPANY_ID)
                .name("배우자 출산휴가 정책")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }

    private Policy createChildcareLeavePolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(365.0); // 남녀고용평등법 제19조 - 최대 1년
        leaveRule.setAccrualType("MANUAL");
        leaveRule.setMinimumRequestUnit("DAY");

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.CHILDCARE_LEAVE))
                .companyId(COMPANY_ID)
                .name("육아휴직 정책")
                .ruleDetails(ruleDetails)
                .isPaid(false) // 육아휴직은 일반적으로 무급 또는 정부 지원
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }

    private Policy createFamilyCareLeavePolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(10.0); // 남녀고용평등법 제22조의2 - 연간 최대 10일
        leaveRule.setAccrualType("AUTO");
        leaveRule.setMinimumRequestUnit("DAY");

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.FAMILY_CARE_LEAVE))
                .companyId(COMPANY_ID)
                .name("가족돌봄휴가 정책")
                .ruleDetails(ruleDetails)
                .isPaid(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }

    private Policy createMenstrualLeavePolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(1.0); // 근로기준법 제73조 - 월 1일
        leaveRule.setAccrualType("AUTO");
        leaveRule.setMinimumRequestUnit("DAY");

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.MENSTRUAL_LEAVE))
                .companyId(COMPANY_ID)
                .name("생리휴가 정책")
                .ruleDetails(ruleDetails)
                .isPaid(false) // 무급 휴가
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }

    private Policy createBusinessTripPolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        TripRuleDto tripRule = new TripRuleDto();
        tripRule.setType("DOMESTIC"); // 국내 출장
        tripRule.setPerDiemAmount(new BigDecimal("50000")); // 일비 5만원
        tripRule.setAccommodationLimit(new BigDecimal("100000")); // 숙박비 한도 10만원
        tripRule.setTransportationLimit(new BigDecimal("200000")); // 교통비 한도 20만원

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setTripRule(tripRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.BUSINESS_TRIP))
                .companyId(COMPANY_ID)
                .name("국내 출장 정책")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }

    private Policy createOvertimePolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        OvertimeRuleDto overtimeRule = new OvertimeRuleDto();
        overtimeRule.setAllowOvertime(true);
        overtimeRule.setMaxWeeklyOvertimeMinutes(720); // 근로기준법 제53조 - 주 12시간
        overtimeRule.setOvertimeRate(new BigDecimal("1.5")); // 근로기준법 제56조 - 1.5배

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setOvertimeRule(overtimeRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.OVERTIME))
                .companyId(COMPANY_ID)
                .name("연장근무 정책")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }

    private Policy createNightWorkPolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        OvertimeRuleDto overtimeRule = new OvertimeRuleDto();
        overtimeRule.setAllowNightWork(true);
        overtimeRule.setNightWorkRate(new BigDecimal("1.5")); // 근로기준법 제56조 - 1.5배

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setOvertimeRule(overtimeRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.NIGHT_WORK))
                .companyId(COMPANY_ID)
                .name("야간근무 정책 (22:00~06:00)")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }

    private Policy createHolidayWorkPolicy(Map<PolicyTypeCode, PolicyType> policyTypes) {
        OvertimeRuleDto overtimeRule = new OvertimeRuleDto();
        overtimeRule.setAllowHolidayWork(true);
        overtimeRule.setHolidayWorkRate(new BigDecimal("1.5")); // 8시간 이내 1.5배
        overtimeRule.setHolidayOvertimeRate(new BigDecimal("2.0")); // 8시간 초과 2.0배

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setOvertimeRule(overtimeRule);

        return Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.HOLIDAY_WORK))
                .companyId(COMPANY_ID)
                .name("휴일근무 정책")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build();
    }
}
