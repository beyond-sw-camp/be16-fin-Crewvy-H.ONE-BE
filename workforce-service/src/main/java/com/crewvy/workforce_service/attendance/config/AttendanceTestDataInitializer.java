package com.crewvy.workforce_service.attendance.config;

import com.crewvy.workforce_service.attendance.constant.*;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.*;
import com.crewvy.workforce_service.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 시연용 테스트 데이터 초기화 (대량 데이터 생성)
 *
 * 생성 데이터:
 * - 과거 60일 + 미래 30일의 출퇴근 기록
 * - 다양한 신청 유형 (연차, 반차, 시차, 출장, 휴직, 연장근무, 야간근무, 휴일근무)
 * - 모든 신청은 APPROVED 상태 (승인 시스템 미구현)
 * - 연차 잔액은 실제 사용량 반영
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceTestDataInitializer implements CommandLineRunner {

    private final PolicyTypeRepository policyTypeRepository;
    private final PolicyRepository policyRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final WorkLocationRepository workLocationRepository;
    private final MemberBalanceRepository memberBalanceRepository;
    private final RequestRepository requestRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;

    // H.ONE 컴퍼니 ID (member-service의 AutoCreateAdmin에서 생성됨)
    private static final UUID COMPANY_ID = UUID.fromString("12b9423d-beda-4473-a323-99a376225c0f");

    // 테스트용 멤버 ID들 (AutoCreateAdmin에서 생성된 emp1~10@h.one)
    private static final UUID HR_ADMIN_ID = UUID.fromString("0853b214-9958-4765-bdb2-cd4ea555bca4"); // emp1 - 김민준 (관리자)
    private static final UUID HR_MEMBER1_ID = UUID.fromString("7ad02525-b869-4a52-926a-0461723efce7"); // emp2 - 이서준 (지각 잦음)
    private static final UUID HR_MEMBER2_ID = UUID.fromString("be05c8d4-3f1d-42e0-8b9e-6243fdceb3bc"); // emp3 - 박도윤 (출산휴가)
    private static final UUID HR_MEMBER3_ID = UUID.fromString("4fe0d111-a3bc-48e3-8777-f3b3e665032c"); // emp4 - 최시우 (육아휴직 분할 사용)
    private static final UUID HR_MEMBER4_ID = UUID.fromString("6396a97f-719b-4ff0-8ccf-b6b36a2dd8e4"); // emp5 - 정하준 (생리휴가)
    private static final UUID DEV_MEMBER1_ID = UUID.fromString("5e39204e-e3b2-43d7-bf04-6e918651c452"); // emp6 - 강지호 (초과근무 많음)
    private static final UUID DEV_MEMBER2_ID = UUID.fromString("96175226-4036-4e96-adff-5027810ed366"); // emp7 - 윤은우 (야간근무)
    private static final UUID DEV_MEMBER3_ID = UUID.fromString("ed709d5d-1347-48e7-9e29-9e69f3aa9906"); // emp8 - 임선우 (배우자 출산휴가)
    private static final UUID DEV_MEMBER4_ID = UUID.fromString("3397106b-20ea-4c85-b1a4-9fda0ec6e71f"); // emp9 - 한유찬 (개인 정책)
    private static final UUID DEV_MEMBER5_ID = UUID.fromString("32781e02-4409-4bab-9250-51ec9ccdf387"); // emp10 - 오이안 (가족돌봄)

    private static final List<UUID> ALL_MEMBERS = List.of(
            HR_ADMIN_ID, HR_MEMBER1_ID, HR_MEMBER2_ID, HR_MEMBER3_ID, HR_MEMBER4_ID,
            DEV_MEMBER1_ID, DEV_MEMBER2_ID, DEV_MEMBER3_ID, DEV_MEMBER4_ID, DEV_MEMBER5_ID
    );

    // 2025년 대한민국 공휴일
    private static final Set<LocalDate> HOLIDAYS_2025 = Set.of(
            LocalDate.of(2025, 1, 1),   // 신정
            LocalDate.of(2025, 1, 28),  // 설날 전날
            LocalDate.of(2025, 1, 29),  // 설날
            LocalDate.of(2025, 1, 30),  // 설날 다음날
            LocalDate.of(2025, 3, 1),   // 삼일절
            LocalDate.of(2025, 5, 5),   // 어린이날
            LocalDate.of(2025, 6, 6),   // 현충일
            LocalDate.of(2025, 8, 15),  // 광복절
            LocalDate.of(2025, 9, 6),   // 추석 전날
            LocalDate.of(2025, 9, 7),   // 추석
            LocalDate.of(2025, 9, 8),   // 추석 다음날
            LocalDate.of(2025, 10, 3),  // 개천절
            LocalDate.of(2025, 10, 9),  // 한글날
            LocalDate.of(2025, 12, 25)  // 크리스마스
    );

    private Random random = new Random(42); // 재현 가능한 랜덤

    @Override
    @Transactional
    public void run(String... args) {
        // 이미 데이터가 있으면 스킵
        if (policyTypeRepository.countByCompanyId(COMPANY_ID) > 0) {
            log.info("✅ 근태 테스트 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("🚀 시연용 대량 근태 데이터 초기화 시작...");
        log.info("📅 데이터 범위: {} ~ {}", LocalDate.now().minusDays(60), LocalDate.now().plusDays(30));
        log.info("📝 핵심 테스트 케이스:");
        log.info("   - 육아휴직 3회 분할 사용 (최시우)");
        log.info("   - 출산휴가 2회 분할 사용 (박도윤)");
        log.info("   - 배우자출산/가족돌봄/생리휴가 케이스");
        log.info("   - 주간 초과근무 12시간 한도 테스트 (강지호)");
        log.info("   - 계층적 정책 할당 (회사/개인 레벨)");
        log.info("   - PENDING/APPROVED 상태 혼합");

        // 1. 근무지 생성
        List<WorkLocation> workLocations = createWorkLocations();
        log.info("✅ 근무지 {} 개 생성 완료", workLocations.size());

        // 2. 정책 유형 생성
        Map<PolicyTypeCode, PolicyType> policyTypes = createPolicyTypes();
        log.info("✅ 정책 유형 {} 개 생성 완료", policyTypes.size());

        // 3. 정책 생성
        Map<PolicyTypeCode, Policy> policies = createPolicies(policyTypes, workLocations);
        log.info("✅ 정책 {} 개 생성 완료", policies.size());

        // 4. 정책 할당 (전사 적용)
        assignPoliciesToAllMembers(policies);
        log.info("✅ 정책 할당 완료 (전사 적용)");

        // 5. 휴가 신청 및 승인 (연차 차감 포함)
        Map<UUID, Double> annualLeaveUsage = createDiverseRequests(policies);
        log.info("✅ 다양한 신청 데이터 {}건 생성 완료", requestRepository.count());

        // 6. 연차 잔액 생성 (실제 사용량 반영)
        createMemberBalances(policyTypes, annualLeaveUsage);
        log.info("✅ 연차 잔액 생성 완료 (사용량 반영)");

        // 7. 과거 출퇴근 기록 대량 생성
        Set<LocalDate> leaveDates = getLeaveDatesFromRequests();
        createRealisticAttendanceHistory(leaveDates);
        log.info("✅ 과거 출퇴근 기록 {}건 생성 완료", dailyAttendanceRepository.count());

        log.info("🎉 시연용 대량 데이터 초기화 완료!");
        log.info("📋 시연 계정: emp1@h.one ~ emp10@h.one (비밀번호: 12341234)");
        log.info("📊 생성된 데이터: 근태기록 {}건, 신청 {}건 (PENDING {}건, APPROVED {}건)",
                dailyAttendanceRepository.count(),
                requestRepository.count(),
                requestRepository.findAll().stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count(),
                requestRepository.findAll().stream().filter(r -> r.getStatus() == RequestStatus.APPROVED).count());
        log.info("👥 계정별 특징:");
        log.info("   emp3 (박도윤): 출산휴가 2회 분할 | emp4 (최시우): 육아휴직 3회 분할 중 2회 완료");
        log.info("   emp5 (정하준): 생리휴가 사용 | emp6 (강지호): 주간 초과근무 11시간");
        log.info("   emp8 (임선우): 배우자 출산휴가 | emp10 (오이안): 가족돌봄휴가");
    }

    /**
     * 근무지 생성 (로컬 개발 환경 포함)
     */
    private List<WorkLocation> createWorkLocations() {
        List<WorkLocation> locations = new ArrayList<>();

        // 로컬 개발 테스트용 근무지 (시연용)
        locations.add(WorkLocation.builder()
                .companyId(COMPANY_ID)
                .name("테스트근무지")
                .address("로컬 개발 환경")
                .latitude(37.0081792)
                .longitude(127.0972416)
                .gpsRadius(5000)
                .ipAddress("127.0.0.1") // 로컬호스트
                .isActive(true)
                .description("로컬 개발 환경 테스트용. GPS + IP(localhost) 인증")
                .build());

        // 서울 본사
        locations.add(WorkLocation.builder()
                .companyId(COMPANY_ID)
                .name("서울 본사")
                .address("서울시 강남구 테헤란로 123")
                .latitude(37.5041)
                .longitude(127.0442)
                .gpsRadius(300)
                .ipAddress("192.168.1.0/24")
                .wifiSsid("HONE-Office")
                .isActive(true)
                .description("서울 본사 사무실")
                .build());

        return workLocationRepository.saveAll(locations);
    }

    /**
     * 정책 유형 생성
     */
    private Map<PolicyTypeCode, PolicyType> createPolicyTypes() {
        List<PolicyType> types = List.of(
                // 휴가/휴직 (잔액 관리)
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.ANNUAL_LEAVE).typeName("연차유급휴가").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.MATERNITY_LEAVE).typeName("출산휴가").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.PATERNITY_LEAVE).typeName("배우자출산휴가").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.CHILDCARE_LEAVE).typeName("육아휴직").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.FAMILY_CARE_LEAVE).typeName("가족돌봄휴가").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.MENSTRUAL_LEAVE).typeName("생리휴가").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),

                // 근무
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.STANDARD_WORK).typeName("기본근무").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(3).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.BUSINESS_TRIP).typeName("출장").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(2).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.OVERTIME).typeName("연장근무").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(3).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.NIGHT_WORK).typeName("야간근무").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(3).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.HOLIDAY_WORK).typeName("휴일근무").balanceDeductible(false).categoryCode(PolicyCategory.WORK_SCHEDULE).priority(3).build()
        );

        return policyTypeRepository.saveAll(types).stream()
                .collect(Collectors.toMap(PolicyType::getTypeCode, t -> t));
    }

    /**
     * 정책 생성
     */
    private Map<PolicyTypeCode, Policy> createPolicies(Map<PolicyTypeCode, PolicyType> policyTypes, List<WorkLocation> workLocations) {
        List<Policy> policies = new ArrayList<>();

        // ========== StandardWork 정책 ==========
        WorkTimeRuleDto workTimeRule = new WorkTimeRuleDto();
        workTimeRule.setType("FIXED");
        workTimeRule.setWorkStartTime("09:00");
        workTimeRule.setWorkEndTime("18:00");
        workTimeRule.setFixedWorkMinutes(480);

        BreakRuleDto breakRule = new BreakRuleDto();
        breakRule.setType("AUTO");
        breakRule.setDefaultBreakMinutesFor8Hours(60);
        breakRule.setMandatoryBreakMinutes(60);

        OvertimeRuleDto overtimeRule = new OvertimeRuleDto();
        overtimeRule.setAllowOvertime(true);
        overtimeRule.setAllowNightWork(true);
        overtimeRule.setAllowHolidayWork(true);
        overtimeRule.setMaxWeeklyOvertimeMinutes(720);
        overtimeRule.setOvertimeRate(new BigDecimal("1.5"));
        overtimeRule.setNightWorkRate(new BigDecimal("1.5"));
        overtimeRule.setHolidayWorkRate(new BigDecimal("1.5"));

        AuthRuleDto authRule = new AuthRuleDto();
        authRule.setAllowedWorkLocationIds(workLocations.stream().map(WorkLocation::getId).collect(Collectors.toList()));

        PolicyRuleDetails standardRuleDetails = new PolicyRuleDetails();
        standardRuleDetails.setWorkTimeRule(workTimeRule);
        standardRuleDetails.setBreakRule(breakRule);
        standardRuleDetails.setOvertimeRule(overtimeRule);
        standardRuleDetails.setAuthRule(authRule);

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.STANDARD_WORK))
                .companyId(COMPANY_ID)
                .name("[시연용] 9-6 고정근무 (AUTO 휴게)")
                .ruleDetails(standardRuleDetails)
                .isPaid(true)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        // ========== 연차 정책 ==========
        LeaveRuleDto annualLeaveRule = new LeaveRuleDto();
        annualLeaveRule.setDefaultDays(15.0);
        annualLeaveRule.setAllowedRequestUnits(List.of("DAY", "HALF_DAY_AM", "HALF_DAY_PM", "TIME_OFF"));
        annualLeaveRule.setRequestDeadlineDays(1);
        annualLeaveRule.setAllowRetrospectiveRequest(true);
        annualLeaveRule.setRetrospectiveRequestDays(7);

        PolicyRuleDetails annualRuleDetails = new PolicyRuleDetails();
        annualRuleDetails.setLeaveRule(annualLeaveRule);

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.ANNUAL_LEAVE))
                .companyId(COMPANY_ID)
                .name("2025년 연차 정책")
                .ruleDetails(annualRuleDetails)
                .isPaid(true)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .effectiveTo(LocalDate.of(2025, 12, 31))
                .isActive(true)
                .build());

        // ========== 출산휴가 정책 (90일, 분할 가능) ==========
        LeaveRuleDto maternityLeaveRule = new LeaveRuleDto();
        maternityLeaveRule.setDefaultDays(90.0);
        maternityLeaveRule.setMaxSplitCount(2); // 2회 분할 가능
        maternityLeaveRule.setAllowedRequestUnits(List.of("DAY"));
        maternityLeaveRule.setRequestDeadlineDays(1);
        maternityLeaveRule.setAllowRetrospectiveRequest(true);

        PolicyRuleDetails maternityRuleDetails = new PolicyRuleDetails();
        maternityRuleDetails.setLeaveRule(maternityLeaveRule);

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.MATERNITY_LEAVE))
                .companyId(COMPANY_ID)
                .name("출산휴가 정책 (90일, 2회 분할)")
                .ruleDetails(maternityRuleDetails)
                .isPaid(true)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        // ========== 배우자 출산휴가 정책 (10일) ==========
        LeaveRuleDto paternityLeaveRule = new LeaveRuleDto();
        paternityLeaveRule.setDefaultDays(10.0);
        paternityLeaveRule.setAllowedRequestUnits(List.of("DAY"));
        paternityLeaveRule.setRequestDeadlineDays(1);

        PolicyRuleDetails paternityRuleDetails = new PolicyRuleDetails();
        paternityRuleDetails.setLeaveRule(paternityLeaveRule);

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.PATERNITY_LEAVE))
                .companyId(COMPANY_ID)
                .name("배우자 출산휴가 정책 (10일)")
                .ruleDetails(paternityRuleDetails)
                .isPaid(true)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        // ========== 육아휴직 정책 (365일, 3회 분할 가능) ==========
        LeaveRuleDto childcareLeaveRule = new LeaveRuleDto();
        childcareLeaveRule.setDefaultDays(365.0);
        childcareLeaveRule.setMaxSplitCount(3); // 3회 분할 가능
        childcareLeaveRule.setAllowedRequestUnits(List.of("DAY"));
        childcareLeaveRule.setRequestDeadlineDays(7);
        childcareLeaveRule.setAllowRetrospectiveRequest(false);

        PolicyRuleDetails childcareRuleDetails = new PolicyRuleDetails();
        childcareRuleDetails.setLeaveRule(childcareLeaveRule);

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.CHILDCARE_LEAVE))
                .companyId(COMPANY_ID)
                .name("육아휴직 정책 (365일, 3회 분할)")
                .ruleDetails(childcareRuleDetails)
                .isPaid(false)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        // ========== 가족돌봄휴가 정책 (10일) ==========
        LeaveRuleDto familyCareLeaveRule = new LeaveRuleDto();
        familyCareLeaveRule.setDefaultDays(10.0);
        familyCareLeaveRule.setAllowedRequestUnits(List.of("DAY", "HALF_DAY_AM", "HALF_DAY_PM"));
        familyCareLeaveRule.setRequestDeadlineDays(1);

        PolicyRuleDetails familyCareRuleDetails = new PolicyRuleDetails();
        familyCareRuleDetails.setLeaveRule(familyCareLeaveRule);

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.FAMILY_CARE_LEAVE))
                .companyId(COMPANY_ID)
                .name("가족돌봄휴가 정책 (10일)")
                .ruleDetails(familyCareRuleDetails)
                .isPaid(true)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        // ========== 생리휴가 정책 (월 1일) ==========
        LeaveRuleDto menstrualLeaveRule = new LeaveRuleDto();
        menstrualLeaveRule.setDefaultDays(12.0); // 연 12일 (월 1일)
        menstrualLeaveRule.setAllowedRequestUnits(List.of("DAY"));
        menstrualLeaveRule.setRequestDeadlineDays(0); // 당일 신청 가능

        PolicyRuleDetails menstrualRuleDetails = new PolicyRuleDetails();
        menstrualRuleDetails.setLeaveRule(menstrualLeaveRule);

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.MENSTRUAL_LEAVE))
                .companyId(COMPANY_ID)
                .name("생리휴가 정책 (월 1일)")
                .ruleDetails(menstrualRuleDetails)
                .isPaid(false)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        // ========== 연장근무/야간근무/휴일근무 정책 ==========
        PolicyRuleDetails overtimeOnlyDetails = new PolicyRuleDetails();
        OvertimeRuleDto overtimeOnlyRule = new OvertimeRuleDto();
        overtimeOnlyRule.setAllowOvertime(true);
        overtimeOnlyRule.setMaxWeeklyOvertimeMinutes(720);
        overtimeOnlyRule.setOvertimeRate(new BigDecimal("1.5"));
        overtimeOnlyDetails.setOvertimeRule(overtimeOnlyRule);

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.OVERTIME))
                .companyId(COMPANY_ID)
                .name("연장근무 정책")
                .ruleDetails(overtimeOnlyDetails)
                .isPaid(true)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.NIGHT_WORK))
                .companyId(COMPANY_ID)
                .name("야간근무 정책")
                .ruleDetails(overtimeOnlyDetails)
                .isPaid(true)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.HOLIDAY_WORK))
                .companyId(COMPANY_ID)
                .name("휴일근무 정책")
                .ruleDetails(overtimeOnlyDetails)
                .isPaid(true)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        // ========== 출장 정책 ==========
        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.BUSINESS_TRIP))
                .companyId(COMPANY_ID)
                .name("국내 출장 정책")
                .ruleDetails(new PolicyRuleDetails())
                .isPaid(true)
                .autoApprove(false)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .isActive(true)
                .build());

        return policyRepository.saveAll(policies).stream()
                .collect(Collectors.toMap(p -> p.getPolicyType().getTypeCode(), p -> p));
    }

    /**
     * 정책 할당 (계층 구조 테스트: 회사/조직/개인)
     */
    private void assignPoliciesToAllMembers(Map<PolicyTypeCode, Policy> policies) {
        List<PolicyAssignment> assignments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 회사 레벨 할당 (모든 직원에게 적용)
        for (Policy policy : policies.values()) {
            // 특정 개인만 사용하는 정책은 회사 레벨에서 제외
            if (policy.getPolicyType().getTypeCode() == PolicyTypeCode.MATERNITY_LEAVE ||
                policy.getPolicyType().getTypeCode() == PolicyTypeCode.PATERNITY_LEAVE ||
                policy.getPolicyType().getTypeCode() == PolicyTypeCode.MENSTRUAL_LEAVE) {
                continue;
            }

            assignments.add(PolicyAssignment.builder()
                    .policy(policy)
                    .scopeType(PolicyScopeType.COMPANY)
                    .targetId(COMPANY_ID)
                    .assignedBy(HR_ADMIN_ID)
                    .assignedAt(now)
                    .isActive(true)
                    .build());
        }

        // 2. 개인 레벨 할당 (특정 직원에게만)
        // 박도윤(HR_MEMBER2)에게 출산휴가 정책 할당
        assignments.add(PolicyAssignment.builder()
                .policy(policies.get(PolicyTypeCode.MATERNITY_LEAVE))
                .scopeType(PolicyScopeType.MEMBER)
                .targetId(HR_MEMBER2_ID)
                .assignedBy(HR_ADMIN_ID)
                .assignedAt(now)
                .isActive(true)
                .build());

        // 임선우(DEV_MEMBER3)에게 배우자 출산휴가 정책 할당
        assignments.add(PolicyAssignment.builder()
                .policy(policies.get(PolicyTypeCode.PATERNITY_LEAVE))
                .scopeType(PolicyScopeType.MEMBER)
                .targetId(DEV_MEMBER3_ID)
                .assignedBy(HR_ADMIN_ID)
                .assignedAt(now)
                .isActive(true)
                .build());

        // 정하준(HR_MEMBER4)에게 생리휴가 정책 할당
        assignments.add(PolicyAssignment.builder()
                .policy(policies.get(PolicyTypeCode.MENSTRUAL_LEAVE))
                .scopeType(PolicyScopeType.MEMBER)
                .targetId(HR_MEMBER4_ID)
                .assignedBy(HR_ADMIN_ID)
                .assignedAt(now)
                .isActive(true)
                .build());

        policyAssignmentRepository.saveAll(assignments);
        log.info("✅ 정책 할당 완료: 회사 레벨 {}건, 개인 레벨 3건 (출산/배우자출산/생리휴가)",
                assignments.size() - 3);
    }

    /**
     * 다양한 신청 생성 (모두 APPROVED 상태)
     * @return 멤버별 연차 사용 일수
     */
    private Map<UUID, Double> createDiverseRequests(Map<PolicyTypeCode, Policy> policies) {
        List<Request> requests = new ArrayList<>();
        Map<UUID, Double> annualLeaveUsage = new HashMap<>();

        LocalDate startDate = LocalDate.now().minusDays(60);
        LocalDate endDate = LocalDate.now().plusDays(30);

        Policy annualPolicy = policies.get(PolicyTypeCode.ANNUAL_LEAVE);
        Policy tripPolicy = policies.get(PolicyTypeCode.BUSINESS_TRIP);
        Policy overtimePolicy = policies.get(PolicyTypeCode.OVERTIME);
        Policy nightWorkPolicy = policies.get(PolicyTypeCode.NIGHT_WORK);
        Policy holidayWorkPolicy = policies.get(PolicyTypeCode.HOLIDAY_WORK);
        Policy childcarePolicy = policies.get(PolicyTypeCode.CHILDCARE_LEAVE);

        // 각 직원별 신청 생성
        for (UUID memberId : ALL_MEMBERS) {
            double totalUsed = 0.0;

            // 1. 연차 신청 (월 2-3개)
            LocalDate current = startDate;
            while (current.isBefore(endDate)) {
                if (random.nextDouble() < 0.15) { // 15% 확률로 연차
                    RequestUnit unit = randomLeaveUnit();
                    LocalDateTime start, end;
                    double deduction;

                    switch (unit) {
                        case DAY:
                            int days = random.nextInt(3) + 1; // 1-3일
                            start = current.atStartOfDay();
                            end = current.plusDays(days - 1).atTime(23, 59);
                            deduction = days;
                            break;
                        case HALF_DAY_AM:
                        case HALF_DAY_PM:
                            start = current.atStartOfDay();
                            end = current.atTime(23, 59);
                            deduction = 0.5;
                            break;
                        case TIME_OFF:
                            start = current.atTime(14, 0);
                            end = current.atTime(16, 0);
                            deduction = Math.round((120.0 / 420.0) * 100) / 100.0; // 2시간 = 0.29일
                            break;
                        default:
                            continue;
                    }

                    if (totalUsed + deduction <= 15.0) { // 연차 한도 체크
                        requests.add(Request.builder()
                                .memberId(memberId)
                                .policy(annualPolicy)
                                .requestUnit(unit)
                                .startDateTime(start)
                                .endDateTime(end)
                                .deductionDays(deduction)
                                .reason("개인 사유")
                                .status(RequestStatus.APPROVED)
                                .build());
                        totalUsed += deduction;
                    }
                }
                current = current.plusDays(random.nextInt(10) + 5); // 5-15일 후 다음 신청
            }

            annualLeaveUsage.put(memberId, totalUsed);

            // 2. 출장 신청 (HR팀만, 월 1회)
            if (isHRMember(memberId)) {
                LocalDate tripDate = startDate.plusDays(random.nextInt(30));
                requests.add(Request.builder()
                        .memberId(memberId)
                        .policy(tripPolicy)
                        .requestUnit(RequestUnit.DAY)
                        .startDateTime(tripDate.atTime(9, 0))
                        .endDateTime(tripDate.plusDays(2).atTime(18, 0))
                        .deductionDays(0.0)
                        .reason("고객사 방문")
                        .status(RequestStatus.APPROVED)
                        .build());
            }

            // 3. 연장근무/야간근무 신청 (개발팀만, 주 2-3회)
            if (isDevMember(memberId)) {
                current = startDate;
                while (current.isBefore(LocalDate.now().minusDays(1))) {
                    if (current.getDayOfWeek() != DayOfWeek.SATURDAY &&
                        current.getDayOfWeek() != DayOfWeek.SUNDAY &&
                        random.nextDouble() < 0.3) { // 30% 확률

                        requests.add(Request.builder()
                                .memberId(memberId)
                                .policy(overtimePolicy)
                                .requestUnit(RequestUnit.DAY)
                                .startDateTime(current.atTime(18, 0))
                                .endDateTime(current.atTime(20, 0))
                                .deductionDays(0.0)
                                .reason("프로젝트 마감")
                                .status(RequestStatus.APPROVED)
                                .build());
                    }
                    current = current.plusDays(1);
                }

                // 야간근무 (월 2회)
                LocalDate nightDate1 = startDate.plusDays(random.nextInt(30));
                requests.add(Request.builder()
                        .memberId(memberId)
                        .policy(nightWorkPolicy)
                        .requestUnit(RequestUnit.DAY)
                        .startDateTime(nightDate1.atTime(22, 0))
                        .endDateTime(nightDate1.plusDays(1).atTime(2, 0))
                        .deductionDays(0.0)
                        .reason("긴급 장애 처리")
                        .status(RequestStatus.APPROVED)
                        .build());
            }

            // 4. 휴일근무 (개발팀 일부만, 월 1회)
            if (memberId.equals(DEV_MEMBER1_ID) || memberId.equals(DEV_MEMBER2_ID)) {
                LocalDate holidayDate = findNextWeekend(startDate);
                requests.add(Request.builder()
                        .memberId(memberId)
                        .policy(holidayWorkPolicy)
                        .requestUnit(RequestUnit.DAY)
                        .startDateTime(holidayDate.atTime(10, 0))
                        .endDateTime(holidayDate.atTime(15, 0))
                        .deductionDays(0.0)
                        .reason("서버 점검")
                        .status(RequestStatus.APPROVED)
                        .build());
            }
        }

        // ========== 5. 분할 사용 케이스 (핵심 테스트!) ==========
        Policy maternityPolicy = policies.get(PolicyTypeCode.MATERNITY_LEAVE);
        Policy paternityPolicy = policies.get(PolicyTypeCode.PATERNITY_LEAVE);
        Policy familyCarePolicy = policies.get(PolicyTypeCode.FAMILY_CARE_LEAVE);
        Policy menstrualPolicy = policies.get(PolicyTypeCode.MENSTRUAL_LEAVE);

        // 5-1. 육아휴직 3회 분할 사용 (HR_MEMBER3 - 최시우)
        // 1차: 2024년 9월 (30일) - APPROVED
        LocalDate childcare1Start = LocalDate.now().minusMonths(3);
        LocalDate childcare1End = childcare1Start.plusDays(29);
        requests.add(Request.builder()
                .memberId(HR_MEMBER3_ID)
                .policy(childcarePolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(childcare1Start.atStartOfDay())
                .endDateTime(childcare1End.atTime(23, 59))
                .deductionDays(30.0)
                .reason("육아휴직 1차 (3회 분할 중 1회차)")
                .status(RequestStatus.APPROVED)
                .build());

        // 2차: 2024년 12월 (45일) - APPROVED
        LocalDate childcare2Start = LocalDate.now().minusDays(50);
        LocalDate childcare2End = LocalDate.now().minusDays(5);
        requests.add(Request.builder()
                .memberId(HR_MEMBER3_ID)
                .policy(childcarePolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(childcare2Start.atStartOfDay())
                .endDateTime(childcare2End.atTime(23, 59))
                .deductionDays(45.0)
                .reason("육아휴직 2차 (3회 분할 중 2회차)")
                .status(RequestStatus.APPROVED)
                .build());

        // 3차: 미래 예정 (290일 남음) - PENDING
        LocalDate childcare3Start = LocalDate.now().plusDays(30);
        LocalDate childcare3End = childcare3Start.plusDays(289);
        requests.add(Request.builder()
                .memberId(HR_MEMBER3_ID)
                .policy(childcarePolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(childcare3Start.atStartOfDay())
                .endDateTime(childcare3End.atTime(23, 59))
                .deductionDays(290.0)
                .reason("육아휴직 3차 (3회 분할 중 3회차 - 최종)")
                .status(RequestStatus.PENDING)
                .build());

        // 5-2. 출산휴가 2회 분할 사용 (HR_MEMBER2 - 박도윤)
        // 1차: 60일 (APPROVED)
        LocalDate maternity1Start = LocalDate.now().minusDays(90);
        LocalDate maternity1End = maternity1Start.plusDays(59);
        requests.add(Request.builder()
                .memberId(HR_MEMBER2_ID)
                .policy(maternityPolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(maternity1Start.atStartOfDay())
                .endDateTime(maternity1End.atTime(23, 59))
                .deductionDays(60.0)
                .reason("출산휴가 1차 (2회 분할 중 1회차)")
                .status(RequestStatus.APPROVED)
                .build());

        // 2차: 30일 (APPROVED)
        LocalDate maternity2Start = LocalDate.now().minusDays(25);
        LocalDate maternity2End = LocalDate.now().plusDays(4);
        requests.add(Request.builder()
                .memberId(HR_MEMBER2_ID)
                .policy(maternityPolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(maternity2Start.atStartOfDay())
                .endDateTime(maternity2End.atTime(23, 59))
                .deductionDays(30.0)
                .reason("출산휴가 2차 (2회 분할 중 2회차 - 최종)")
                .status(RequestStatus.APPROVED)
                .build());

        // 5-3. 배우자 출산휴가 (DEV_MEMBER3 - 임선우)
        LocalDate paternityStart = LocalDate.now().minusDays(7);
        LocalDate paternityEnd = paternityStart.plusDays(4); // 5일 사용
        requests.add(Request.builder()
                .memberId(DEV_MEMBER3_ID)
                .policy(paternityPolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(paternityStart.atStartOfDay())
                .endDateTime(paternityEnd.atTime(23, 59))
                .deductionDays(5.0)
                .reason("첫째 자녀 출생")
                .status(RequestStatus.APPROVED)
                .build());

        // 5-4. 가족돌봄휴가 (DEV_MEMBER5 - 오이안)
        // 1월 - 반차 2회 (APPROVED)
        requests.add(Request.builder()
                .memberId(DEV_MEMBER5_ID)
                .policy(familyCarePolicy)
                .requestUnit(RequestUnit.HALF_DAY_AM)
                .startDateTime(LocalDate.now().minusDays(20).atStartOfDay())
                .endDateTime(LocalDate.now().minusDays(20).atTime(12, 0))
                .deductionDays(0.5)
                .reason("부모님 병원 동행")
                .status(RequestStatus.APPROVED)
                .build());

        requests.add(Request.builder()
                .memberId(DEV_MEMBER5_ID)
                .policy(familyCarePolicy)
                .requestUnit(RequestUnit.HALF_DAY_PM)
                .startDateTime(LocalDate.now().minusDays(10).atTime(13, 0))
                .endDateTime(LocalDate.now().minusDays(10).atTime(23, 59))
                .deductionDays(0.5)
                .reason("부모님 병원 동행")
                .status(RequestStatus.APPROVED)
                .build());

        // 5-5. 생리휴가 (HR_MEMBER4 - 정하준)
        // 지난 2개월 사용 (월 1일씩)
        for (int i = 1; i <= 2; i++) {
            LocalDate menstrualDate = LocalDate.now().minusMonths(i).withDayOfMonth(15);
            requests.add(Request.builder()
                    .memberId(HR_MEMBER4_ID)
                    .policy(menstrualPolicy)
                    .requestUnit(RequestUnit.DAY)
                    .startDateTime(menstrualDate.atStartOfDay())
                    .endDateTime(menstrualDate.atTime(23, 59))
                    .deductionDays(1.0)
                    .reason("생리휴가")
                    .status(RequestStatus.APPROVED)
                    .build());
        }

        // ========== 6. 주간 초과근무 한도 테스트 케이스 (DEV_MEMBER1 - 강지호) ==========
        // 이번 주 월~목: 11시간 초과근무 (APPROVED) -> 금요일에 1시간만 더 가능
        LocalDate thisMonday = LocalDate.now().with(DayOfWeek.MONDAY);

        // 월요일: 3시간
        requests.add(Request.builder()
                .memberId(DEV_MEMBER1_ID)
                .policy(overtimePolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(thisMonday.atTime(18, 0))
                .endDateTime(thisMonday.atTime(21, 0))
                .deductionDays(0.0)
                .reason("프로젝트 긴급 대응")
                .status(RequestStatus.APPROVED)
                .build());

        // 화요일: 4시간
        requests.add(Request.builder()
                .memberId(DEV_MEMBER1_ID)
                .policy(overtimePolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(thisMonday.plusDays(1).atTime(18, 0))
                .endDateTime(thisMonday.plusDays(1).atTime(22, 0))
                .deductionDays(0.0)
                .reason("프로젝트 긴급 대응")
                .status(RequestStatus.APPROVED)
                .build());

        // 수요일: 2시간
        requests.add(Request.builder()
                .memberId(DEV_MEMBER1_ID)
                .policy(overtimePolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(thisMonday.plusDays(2).atTime(18, 0))
                .endDateTime(thisMonday.plusDays(2).atTime(20, 0))
                .deductionDays(0.0)
                .reason("프로젝트 긴급 대응")
                .status(RequestStatus.APPROVED)
                .build());

        // 목요일: 2시간 (총 11시간)
        requests.add(Request.builder()
                .memberId(DEV_MEMBER1_ID)
                .policy(overtimePolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(thisMonday.plusDays(3).atTime(18, 0))
                .endDateTime(thisMonday.plusDays(3).atTime(20, 0))
                .deductionDays(0.0)
                .reason("프로젝트 긴급 대응")
                .status(RequestStatus.APPROVED)
                .build());

        // 금요일: 1시간 PENDING (한도 내)
        requests.add(Request.builder()
                .memberId(DEV_MEMBER1_ID)
                .policy(overtimePolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(thisMonday.plusDays(4).atTime(18, 0))
                .endDateTime(thisMonday.plusDays(4).atTime(19, 0))
                .deductionDays(0.0)
                .reason("프로젝트 마무리")
                .status(RequestStatus.PENDING)
                .build());

        // ========== 7. 최근 신청 내역 (PENDING 상태) ==========
        // 다음 주 연차 신청들 (PENDING)
        for (int i = 0; i < 3; i++) {
            UUID randomMember = ALL_MEMBERS.get(random.nextInt(ALL_MEMBERS.size()));
            LocalDate futureLeaveDate = LocalDate.now().plusDays(7 + i * 3);

            requests.add(Request.builder()
                    .memberId(randomMember)
                    .policy(annualPolicy)
                    .requestUnit(RequestUnit.DAY)
                    .startDateTime(futureLeaveDate.atStartOfDay())
                    .endDateTime(futureLeaveDate.atTime(23, 59))
                    .deductionDays(1.0)
                    .reason("개인 일정")
                    .status(RequestStatus.PENDING)
                    .build());
        }

        // 이번 주 반차 신청 (PENDING)
        requests.add(Request.builder()
                .memberId(HR_ADMIN_ID)
                .policy(annualPolicy)
                .requestUnit(RequestUnit.HALF_DAY_PM)
                .startDateTime(LocalDate.now().plusDays(2).atTime(13, 0))
                .endDateTime(LocalDate.now().plusDays(2).atTime(23, 59))
                .deductionDays(0.5)
                .reason("병원 진료")
                .status(RequestStatus.PENDING)
                .build());

        // ========== 8. 미래 일정 (공유 캘린더용) ==========
        LocalDate futureDate1 = LocalDate.now().plusDays(7);
        requests.add(Request.builder()
                .memberId(HR_ADMIN_ID)
                .policy(annualPolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(futureDate1.atStartOfDay())
                .endDateTime(futureDate1.atTime(23, 59))
                .deductionDays(1.0)
                .reason("연말 휴가")
                .status(RequestStatus.APPROVED)
                .build());

        LocalDate futureDate2 = LocalDate.now().plusDays(14);
        requests.add(Request.builder()
                .memberId(DEV_MEMBER1_ID)
                .policy(tripPolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(futureDate2.atTime(9, 0))
                .endDateTime(futureDate2.plusDays(1).atTime(18, 0))
                .deductionDays(0.0)
                .reason("컨퍼런스 참가")
                .status(RequestStatus.APPROVED)
                .build());

        requestRepository.saveAll(requests);
        return annualLeaveUsage;
    }

    /**
     * 신청된 휴가 날짜 추출 (DailyAttendance 생성 시 제외용)
     */
    private Set<LocalDate> getLeaveDatesFromRequests() {
        Set<LocalDate> leaveDates = new HashSet<>();
        List<Request> approvedLeaves = requestRepository.findAll().stream()
                .filter(r -> r.getStatus() == RequestStatus.APPROVED)
                .filter(r -> {
                    PolicyTypeCode typeCode = r.getPolicy().getPolicyType().getTypeCode();
                    return typeCode == PolicyTypeCode.ANNUAL_LEAVE ||
                           typeCode == PolicyTypeCode.MATERNITY_LEAVE ||
                           typeCode == PolicyTypeCode.PATERNITY_LEAVE ||
                           typeCode == PolicyTypeCode.CHILDCARE_LEAVE ||
                           typeCode == PolicyTypeCode.FAMILY_CARE_LEAVE ||
                           typeCode == PolicyTypeCode.MENSTRUAL_LEAVE ||
                           typeCode == PolicyTypeCode.BUSINESS_TRIP;
                })
                .collect(Collectors.toList());

        for (Request req : approvedLeaves) {
            LocalDate start = req.getStartDateTime().toLocalDate();
            LocalDate end = req.getEndDateTime().toLocalDate();

            // 반차는 근태 기록이 있으므로 제외하지 않음
            if (req.getRequestUnit() == RequestUnit.HALF_DAY_AM ||
                req.getRequestUnit() == RequestUnit.HALF_DAY_PM ||
                req.getRequestUnit() == RequestUnit.TIME_OFF) {
                continue;
            }

            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                leaveDates.add(date);
            }
        }

        return leaveDates;
    }

    /**
     * 과거 출퇴근 기록 대량 생성
     */
    private void createRealisticAttendanceHistory(Set<LocalDate> leaveDates) {
        List<AttendanceLog> logs = new ArrayList<>();
        List<DailyAttendance> dailies = new ArrayList<>();

        LocalDate startDate = LocalDate.now().minusDays(60);
        LocalDate endDate = LocalDate.now().minusDays(1);

        for (UUID memberId : ALL_MEMBERS) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                // 주말/공휴일 스킵 (휴일근무 제외)
                if (isWeekend(date) || HOLIDAYS_2025.contains(date)) {
                    // 일부 개발자만 주말 근무
                    if (!shouldWorkOnHoliday(memberId, date)) {
                        continue;
                    }
                }

                // 휴가 날짜 스킵
                if (leaveDates.contains(date)) {
                    continue;
                }

                // 근태 패턴 결정
                AttendancePattern pattern = determinePattern(memberId, date);

                // 출근 시각
                LocalTime clockInTime = pattern.clockInTime;
                LocalTime clockOutTime = pattern.clockOutTime;

                logs.add(AttendanceLog.builder()
                        .memberId(memberId)
                        .eventType(EventType.CLOCK_IN)
                        .eventTime(date.atTime(clockInTime))
                        .build());

                // 외출 (20% 확률)
                Integer totalGoOutMinutes = 0;
                if (random.nextDouble() < 0.2) {
                    LocalTime goOutStart = LocalTime.of(14 + random.nextInt(2), random.nextInt(60));
                    LocalTime goOutEnd = goOutStart.plusMinutes(30 + random.nextInt(30));

                    logs.add(AttendanceLog.builder()
                            .memberId(memberId)
                            .eventType(EventType.GO_OUT)
                            .eventTime(date.atTime(goOutStart))
                            .build());

                    logs.add(AttendanceLog.builder()
                            .memberId(memberId)
                            .eventType(EventType.COME_BACK)
                            .eventTime(date.atTime(goOutEnd))
                            .build());

                    totalGoOutMinutes = (int) java.time.Duration.between(goOutStart, goOutEnd).toMinutes();
                }

                logs.add(AttendanceLog.builder()
                        .memberId(memberId)
                        .eventType(EventType.CLOCK_OUT)
                        .eventTime(date.atTime(clockOutTime))
                        .build());

                // DailyAttendance 계산
                int totalMinutes = (int) java.time.Duration.between(clockInTime, clockOutTime).toMinutes();
                int breakMinutes = 60; // AUTO 모드
                int workedMinutes = totalMinutes - breakMinutes - totalGoOutMinutes;
                int overtimeMinutes = Math.max(0, workedMinutes - 480);

                dailies.add(DailyAttendance.builder()
                        .memberId(memberId)
                        .companyId(COMPANY_ID)
                        .attendanceDate(date)
                        .status(pattern.status)
                        .firstClockIn(date.atTime(clockInTime))
                        .lastClockOut(date.atTime(clockOutTime))
                        .workedMinutes(workedMinutes)
                        .totalBreakMinutes(breakMinutes)
                        .totalGoOutMinutes(totalGoOutMinutes)
                        .overtimeMinutes(overtimeMinutes)
                        .daytimeOvertimeMinutes(overtimeMinutes)
                        .nightWorkMinutes(0)
                        .holidayWorkMinutes(isWeekend(date) || HOLIDAYS_2025.contains(date) ? workedMinutes : 0)
                        .isLate(pattern.isLate)
                        .lateMinutes(pattern.lateMinutes)
                        .isEarlyLeave(pattern.isEarlyLeave)
                        .earlyLeaveMinutes(pattern.earlyLeaveMinutes)
                        .build());
            }
        }

        attendanceLogRepository.saveAll(logs);
        dailyAttendanceRepository.saveAll(dailies);

        log.info("📊 생성된 근태 로그: {}건, 일별 근태: {}건", logs.size(), dailies.size());
    }

    /**
     * 근태 패턴 결정
     */
    private AttendancePattern determinePattern(UUID memberId, LocalDate date) {
        AttendancePattern pattern = new AttendancePattern();

        // 기본 출퇴근 시각
        LocalTime baseClockIn = LocalTime.of(9, 0);
        LocalTime baseClockOut = LocalTime.of(18, 0);

        // 지각 패턴 (10% 확률, 이서준은 20%)
        double lateChance = memberId.equals(HR_MEMBER1_ID) ? 0.2 : 0.1;
        if (random.nextDouble() < lateChance) {
            int lateMinutes = 10 + random.nextInt(30); // 10-40분 지각
            pattern.clockInTime = baseClockIn.plusMinutes(lateMinutes);
            pattern.isLate = true;
            pattern.lateMinutes = lateMinutes;
        } else {
            pattern.clockInTime = baseClockIn.minusMinutes(random.nextInt(15)); // 조금 일찍 출근
        }

        // 조퇴 패턴 (5% 확률)
        if (random.nextDouble() < 0.05) {
            int earlyMinutes = 10 + random.nextInt(50);
            pattern.clockOutTime = baseClockOut.minusMinutes(earlyMinutes);
            pattern.isEarlyLeave = true;
            pattern.earlyLeaveMinutes = earlyMinutes;
        }
        // 연장근무 패턴 (개발자 30%, HR 10%)
        else if (isDevMember(memberId) && random.nextDouble() < 0.3 || random.nextDouble() < 0.1) {
            int overtimeHours = 1 + random.nextInt(4); // 1-4시간
            pattern.clockOutTime = baseClockOut.plusHours(overtimeHours);
        } else {
            pattern.clockOutTime = baseClockOut.plusMinutes(random.nextInt(20)); // 정시 전후
        }

        pattern.status = AttendanceStatus.NORMAL_WORK;
        return pattern;
    }

    /**
     * 연차 잔액 생성 (실제 사용량 반영)
     */
    private void createMemberBalances(Map<PolicyTypeCode, PolicyType> policyTypes, Map<UUID, Double> annualLeaveUsage) {
        PolicyType annualLeaveType = policyTypes.get(PolicyTypeCode.ANNUAL_LEAVE);
        int currentYear = LocalDate.now().getYear();
        List<MemberBalance> balances = new ArrayList<>();

        for (UUID memberId : ALL_MEMBERS) {
            double totalGranted = 15.0;
            double totalUsed = annualLeaveUsage.getOrDefault(memberId, 0.0);
            double remaining = totalGranted - totalUsed;

            balances.add(MemberBalance.builder()
                    .memberId(memberId)
                    .companyId(COMPANY_ID)
                    .year(currentYear)
                    .balanceTypeCode(annualLeaveType.getTypeCode())
                    .totalGranted(totalGranted)
                    .totalUsed(totalUsed)
                    .remaining(remaining)
                    .expirationDate(LocalDate.of(currentYear, 12, 31))
                    .isPaid(true)
                    .build());
        }

        memberBalanceRepository.saveAll(balances);
    }

    // ========== Helper Methods ==========

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private boolean isHRMember(UUID memberId) {
        return memberId.equals(HR_ADMIN_ID) || memberId.equals(HR_MEMBER1_ID) ||
               memberId.equals(HR_MEMBER2_ID) || memberId.equals(HR_MEMBER3_ID) ||
               memberId.equals(HR_MEMBER4_ID);
    }

    private boolean isDevMember(UUID memberId) {
        return memberId.equals(DEV_MEMBER1_ID) || memberId.equals(DEV_MEMBER2_ID) ||
               memberId.equals(DEV_MEMBER3_ID) || memberId.equals(DEV_MEMBER4_ID) ||
               memberId.equals(DEV_MEMBER5_ID);
    }

    private boolean shouldWorkOnHoliday(UUID memberId, LocalDate date) {
        // 강지호, 윤은우만 가끔 주말 근무
        if (memberId.equals(DEV_MEMBER1_ID) || memberId.equals(DEV_MEMBER2_ID)) {
            return random.nextDouble() < 0.1; // 10% 확률
        }
        return false;
    }

    private RequestUnit randomLeaveUnit() {
        double rand = random.nextDouble();
        if (rand < 0.5) return RequestUnit.DAY;
        if (rand < 0.75) return RequestUnit.HALF_DAY_AM;
        if (rand < 0.9) return RequestUnit.HALF_DAY_PM;
        return RequestUnit.TIME_OFF;
    }

    private LocalDate findNextWeekend(LocalDate start) {
        LocalDate current = start;
        while (current.getDayOfWeek() != DayOfWeek.SATURDAY) {
            current = current.plusDays(1);
        }
        return current;
    }

    /**
     * 근태 패턴 DTO
     */
    private static class AttendancePattern {
        LocalTime clockInTime;
        LocalTime clockOutTime;
        AttendanceStatus status;
        boolean isLate = false;
        int lateMinutes = 0;
        boolean isEarlyLeave = false;
        int earlyLeaveMinutes = 0;
    }
}
