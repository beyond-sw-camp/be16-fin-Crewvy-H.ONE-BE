package com.crewvy.workforce_service.attendance.config;

import com.crewvy.workforce_service.attendance.constant.*;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.*;
import com.crewvy.workforce_service.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

//@Component
@RequiredArgsConstructor
public class AttendanceTestDataInitializer implements CommandLineRunner {

    private final PolicyTypeRepository policyTypeRepository;
    private final PolicyRepository policyRepository;
    private final WorkLocationRepository workLocationRepository;
    private final MemberBalanceRepository memberBalanceRepository;
    private final RequestRepository requestRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;

    // member-service의 AutoCreateAdmin에서 생성되는 H.ONE 컴퍼니 ID
    private static final UUID COMPANY_ID = UUID.fromString("8892759c-b28b-4395-a1b4-7ebe86bb65cc");

    // 테스트용 멤버 ID들 (DB에서 확인 후 실제 UUID로 교체 필요)
    // 인사팀 직원 (emp1@h.one - 김민준) - 관리자로 사용
    private static final UUID HR_ADMIN_ID = UUID.fromString("ee088d44-9bc7-417f-911e-cd1fa4b42b1e"); // TODO: DB 확인 후 교체

    // 인사팀 직원 (emp2@h.one - 이서준)
    private static final UUID HR_MEMBER1_ID = UUID.fromString("b523d3ee-8fc5-4c7d-a120-a419cc0d4ef0"); // TODO: DB 확인 후 교체

    // 인사팀 직원 (emp3@h.one - 박도윤)
    private static final UUID HR_MEMBER2_ID = UUID.fromString("abfa6d37-b49b-4358-9e02-49bc0a8b41cd"); // TODO: DB 확인 후 교체

    // 개발팀 직원 (emp6@h.one - 강지호)
    private static final UUID DEV_MEMBER1_ID = UUID.fromString("75953373-c31c-4985-8be2-7e2d969b870d"); // TODO: DB 확인 후 교체

    // 개발팀 직원 (emp7@h.one - 윤은우)
    private static final UUID DEV_MEMBER2_ID = UUID.fromString("9ea19f30-12f8-4bdf-8d45-3e61c0933c2b"); // TODO: DB 확인 후 교체

    @Override
    @Transactional
    public void run(String... args) {
        // H.ONE 컴퍼니에 대한 데이터만 초기화
        if (policyTypeRepository.countByCompanyId(COMPANY_ID) > 0) {
            return;
        }

        // 1. 근무지 (WorkLocation) 먼저 생성 (정책에서 참조하기 위해)
        List<WorkLocation> workLocations = createWorkLocations();

        // 2. 정책 유형 (PolicyType) 생성
        Map<PolicyTypeCode, PolicyType> policyTypes = createPolicyTypes();

        // 3. 정책 (Policy) 생성 (WorkLocation 참조)
        createPolicies(policyTypes, workLocations);

        // 4. 테스트용 MemberBalance 생성 (휴가 신청 테스트용)
        createMemberBalances(policyTypes);

        // 5. 테스트용 디바이스 등록 신청 생성
        createSampleDeviceRequests();

        // 6. 테스트용 근태 기록 (Log & Daily) 생성
        createAttendanceData();
    }

    private void createAttendanceData() {
        List<AttendanceLog> logs = new ArrayList<>();
        List<DailyAttendance> dailies = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // --- 1. 이서준 (HR_MEMBER1_ID): 어제 정상 근무, 오늘 출근 ---
        // 어제 기록
        logs.add(AttendanceLog.builder().memberId(HR_MEMBER1_ID).eventType(EventType.CLOCK_IN).eventTime(yesterday.atTime(8, 58)).build());
        logs.add(AttendanceLog.builder().memberId(HR_MEMBER1_ID).eventType(EventType.BREAK_START).eventTime(yesterday.atTime(12, 30)).build());
        logs.add(AttendanceLog.builder().memberId(HR_MEMBER1_ID).eventType(EventType.BREAK_END).eventTime(yesterday.atTime(13, 32)).build());
        logs.add(AttendanceLog.builder().memberId(HR_MEMBER1_ID).eventType(EventType.CLOCK_OUT).eventTime(yesterday.atTime(18, 5)).build());
        dailies.add(DailyAttendance.builder()
            .memberId(HR_MEMBER1_ID).companyId(COMPANY_ID).attendanceDate(yesterday).status(AttendanceStatus.NORMAL_WORK)
            .firstClockIn(yesterday.atTime(8, 58)).lastClockOut(yesterday.atTime(18, 5))
            .workedMinutes(485).totalBreakMinutes(62).overtimeMinutes(5)
            .daytimeOvertimeMinutes(5).nightWorkMinutes(0).holidayWorkMinutes(0)
            .isLate(false).isEarlyLeave(false).build());
        // 오늘 기록
        logs.add(AttendanceLog.builder().memberId(HR_MEMBER1_ID).eventType(EventType.CLOCK_IN).eventTime(today.atTime(9, 2)).build());
        dailies.add(DailyAttendance.builder()
            .memberId(HR_MEMBER1_ID).companyId(COMPANY_ID).attendanceDate(today).status(AttendanceStatus.NORMAL_WORK)
            .firstClockIn(today.atTime(9, 2)).build());

        // --- 2. 박도윤 (HR_MEMBER2_ID): 어제 지각, 오늘 출근 ---
        // 어제 기록
        logs.add(AttendanceLog.builder().memberId(HR_MEMBER2_ID).eventType(EventType.CLOCK_IN).eventTime(yesterday.atTime(9, 25)).build());
        logs.add(AttendanceLog.builder().memberId(HR_MEMBER2_ID).eventType(EventType.CLOCK_OUT).eventTime(yesterday.atTime(18, 31)).build());
        dailies.add(DailyAttendance.builder()
            .memberId(HR_MEMBER2_ID).companyId(COMPANY_ID).attendanceDate(yesterday).status(AttendanceStatus.NORMAL_WORK)
            .firstClockIn(yesterday.atTime(9, 25)).lastClockOut(yesterday.atTime(18, 31))
            .workedMinutes(486).totalBreakMinutes(60) // 가정
            .overtimeMinutes(6).daytimeOvertimeMinutes(6).nightWorkMinutes(0).holidayWorkMinutes(0)
            .isLate(true).lateMinutes(25).isEarlyLeave(false).build());
        // 오늘 기록
        logs.add(AttendanceLog.builder().memberId(HR_MEMBER2_ID).eventType(EventType.CLOCK_IN).eventTime(today.atTime(8, 55)).build());
        dailies.add(DailyAttendance.builder()
            .memberId(HR_MEMBER2_ID).companyId(COMPANY_ID).attendanceDate(today).status(AttendanceStatus.NORMAL_WORK)
            .firstClockIn(today.atTime(8, 55)).build());

        // --- 3. 강지호 (DEV_MEMBER1_ID): 어제 야근, 오늘 외출 중 ---
        // 어제 기록
        logs.add(AttendanceLog.builder().memberId(DEV_MEMBER1_ID).eventType(EventType.CLOCK_IN).eventTime(yesterday.atTime(9, 5)).build());
        logs.add(AttendanceLog.builder().memberId(DEV_MEMBER1_ID).eventType(EventType.CLOCK_OUT).eventTime(yesterday.atTime(22, 15)).build());
        dailies.add(DailyAttendance.builder()
            .memberId(DEV_MEMBER1_ID).companyId(COMPANY_ID).attendanceDate(yesterday).status(AttendanceStatus.NORMAL_WORK)
            .firstClockIn(yesterday.atTime(9, 5)).lastClockOut(yesterday.atTime(22, 15))
            .workedMinutes(730).totalBreakMinutes(60) // 가정
            .overtimeMinutes(250).daytimeOvertimeMinutes(235).nightWorkMinutes(15).holidayWorkMinutes(0)
            .isLate(true).lateMinutes(5).isEarlyLeave(false).build());
        // 오늘 기록
        logs.add(AttendanceLog.builder().memberId(DEV_MEMBER1_ID).eventType(EventType.CLOCK_IN).eventTime(today.atTime(9, 10)).build());
        logs.add(AttendanceLog.builder().memberId(DEV_MEMBER1_ID).eventType(EventType.GO_OUT).eventTime(today.atTime(14, 0)).build());
        dailies.add(DailyAttendance.builder()
            .memberId(DEV_MEMBER1_ID).companyId(COMPANY_ID).attendanceDate(today).status(AttendanceStatus.NORMAL_WORK)
            .firstClockIn(today.atTime(9, 10)).build());

        // --- 4. 윤은우 (DEV_MEMBER2_ID): 어제 휴가, 오늘 결근 (데이터 없음) ---
        // 어제 기록 (휴가)
        dailies.add(DailyAttendance.builder()
            .memberId(DEV_MEMBER2_ID).companyId(COMPANY_ID).attendanceDate(yesterday).status(AttendanceStatus.ANNUAL_LEAVE)
            .build());

        attendanceLogRepository.saveAll(logs);
        dailyAttendanceRepository.saveAll(dailies);
    }


    private Map<PolicyTypeCode, PolicyType> createPolicyTypes() {
        List<PolicyType> typesToCreate = List.of(
                // 법정 휴가 (Priority 1) - 연차만 잔여일수 차감
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.ANNUAL_LEAVE).typeName("연차유급휴가").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.MATERNITY_LEAVE).typeName("출산전후휴가").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.PATERNITY_LEAVE).typeName("배우자 출산휴가").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.CHILDCARE_LEAVE).typeName("육아휴직").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.FAMILY_CARE_LEAVE).typeName("가족돌봄휴가").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.MENSTRUAL_LEAVE).typeName("생리휴가").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),

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

    private void createPolicies(Map<PolicyTypeCode, PolicyType> policyTypes, List<WorkLocation> workLocations) {
        List<Policy> policies = new ArrayList<>();

        // ========== 1. 기본 근무 (STANDARD_WORK) ==========
        policies.add(createStandardWorkPolicy(policyTypes, workLocations));

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

    private Policy createStandardWorkPolicy(Map<PolicyTypeCode, PolicyType> policyTypes, List<WorkLocation> workLocations) {
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

        // WorkLocation 참조 방식으로 변경
        AuthRuleDto authRule = new AuthRuleDto();
        // 모든 근무지에서 출퇴근 허용 (서울 본사 3층, 4층, 부산 지점)
        authRule.setAllowedWorkLocationIds(
            workLocations.stream().map(WorkLocation::getId).collect(Collectors.toList())
        );
        // GPS + WiFi 인증 필수
        authRule.setRequiredAuthTypes(List.of("GPS", "WIFI"));

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
        leaveRule.setAccrualType("ACCRUAL"); // 자동 발생
        leaveRule.setMinimumRequestUnit("DAY"); // 최소 단위: 1일
        leaveRule.setRequestDeadlineDays(1); // 1일 전 신청

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
        leaveRule.setMinimumRequestUnit("DAY"); // 최소 단위: 1일
        leaveRule.setRequestDeadlineDays(30); // 30일 전 신청

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
        leaveRule.setMinimumRequestUnit("DAY"); // 최소 단위: 1일
        leaveRule.setRequestDeadlineDays(1); // 1일 전 신청
        leaveRule.setMaxDaysFromEventDate(90); // 출산일 기준 ±90일 이내 사용
        leaveRule.setMaxSplitCount(2); // 최대 2회 분할 사용 가능

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
        leaveRule.setMinimumRequestUnit("DAY"); // 최소 단위: 1일
        leaveRule.setRequestDeadlineDays(30); // 30일 전 신청
        leaveRule.setMaxSplitCount(3); // 최대 3회 분할 사용 가능
        leaveRule.setMinConsecutiveDays(30); // 1회당 최소 30일 연속 사용

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
        leaveRule.setMinimumRequestUnit("DAY"); // 최소 단위: 1일
        leaveRule.setRequestDeadlineDays(1); // 1일 전 신청
        leaveRule.setLimitPeriod("YEARLY"); // 연간 제한
        leaveRule.setMaxDaysPerPeriod(10); // 연간 최대 10일

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
        leaveRule.setMinimumRequestUnit("DAY"); // 최소 단위: 1일
        leaveRule.setLimitPeriod("MONTHLY"); // 월간 제한
        leaveRule.setMaxDaysPerPeriod(1); // 월 최대 1일

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

    private List<WorkLocation> createWorkLocations() {
        List<WorkLocation> workLocations = new ArrayList<>();

        // 1. 서울 본사 3층 개발팀 (GPS + WiFi + IP)
        workLocations.add(WorkLocation.builder()
                .companyId(COMPANY_ID)
                .name("서울 본사 3층 개발팀")
                .address("서울시 강남구 테헤란로 123")
                .latitude(37.5041)
                .longitude(127.0442)
                .gpsRadius(300)
                .ipAddress("192.168.3.0/24")
                .wifiSsid("HONE-Dev-3F")
                .wifiBssid("AA:BB:CC:DD:EE:F1")
                .isActive(true)
                .description("3층 개발팀 전용 근무지. WiFi 및 GPS 기반 출퇴근 인증.")
                .build());

        // 2. 서울 본사 4층 마케팅팀 (GPS + WiFi + IP)
        workLocations.add(WorkLocation.builder()
                .companyId(COMPANY_ID)
                .name("서울 본사 4층 마케팅팀")
                .address("서울시 강남구 테헤란로 123")
                .latitude(37.5041)
                .longitude(127.0442)
                .gpsRadius(300)
                .ipAddress("192.168.4.0/24")
                .wifiSsid("HONE-Mkt-4F")
                .wifiBssid("AA:BB:CC:DD:EE:F2")
                .isActive(true)
                .description("4층 마케팅팀 전용 근무지. WiFi 및 GPS 기반 출퇴근 인증.")
                .build());

        // 3. 부산 지점 (GPS만)
        workLocations.add(WorkLocation.builder()
                .companyId(COMPANY_ID)
                .name("부산 지점")
                .address("부산시 해운대구 센텀중앙로 99")
                .latitude(35.1698)
                .longitude(129.1308)
                .gpsRadius(500)
                .ipAddress(null)
                .wifiSsid(null)
                .wifiBssid(null)
                .isActive(true)
                .description("부산 지점. GPS 기반 출퇴근 인증.")
                .build());

        return workLocationRepository.saveAll(workLocations);
    }

    /**
     * 테스트용 MemberBalance 생성
     * 각 직원들에게 연차 잔여일수를 부여하여 휴가 신청 테스트 가능하도록 함
     */
    private void createMemberBalances(Map<PolicyTypeCode, PolicyType> policyTypes) {
        PolicyType annualLeaveType = policyTypes.get(PolicyTypeCode.ANNUAL_LEAVE);
        int currentYear = LocalDate.now().getYear();

        List<MemberBalance> balances = new ArrayList<>();

        // 인사팀 관리자 (emp1@h.one - 김민준) - 15일
        balances.add(MemberBalance.builder()
                .memberId(HR_ADMIN_ID)
                .companyId(COMPANY_ID)
                .year(currentYear)
                .balanceTypeCode(annualLeaveType.getTypeCode())
                .totalGranted(15.0)
                .totalUsed(0.0)
                .remaining(15.0)
                .expirationDate(LocalDate.of(currentYear, 12, 31))
                .isPaid(true)
                .build());

        // 인사팀 직원1 (emp2@h.one - 이서준) - 15일 중 3일 사용
        balances.add(MemberBalance.builder()
                .memberId(HR_MEMBER1_ID)
                .companyId(COMPANY_ID)
                .year(currentYear)
                .balanceTypeCode(annualLeaveType.getTypeCode())
                .totalGranted(15.0)
                .totalUsed(3.0)
                .remaining(12.0)
                .expirationDate(LocalDate.of(currentYear, 12, 31))
                .isPaid(true)
                .build());

        // 인사팀 직원2 (emp3@h.one - 박도윤) - 15일 중 5일 사용
        balances.add(MemberBalance.builder()
                .memberId(HR_MEMBER2_ID)
                .companyId(COMPANY_ID)
                .year(currentYear)
                .balanceTypeCode(annualLeaveType.getTypeCode())
                .totalGranted(15.0)
                .totalUsed(5.0)
                .remaining(10.0)
                .expirationDate(LocalDate.of(currentYear, 12, 31))
                .isPaid(true)
                .build());

        // 개발팀 직원1 (emp6@h.one - 강지호) - 15일 중 7일 사용
        balances.add(MemberBalance.builder()
                .memberId(DEV_MEMBER1_ID)
                .companyId(COMPANY_ID)
                .year(currentYear)
                .balanceTypeCode(annualLeaveType.getTypeCode())
                .totalGranted(15.0)
                .totalUsed(7.0)
                .remaining(8.0)
                .expirationDate(LocalDate.of(currentYear, 12, 31))
                .isPaid(true)
                .build());

        // 개발팀 직원2 (emp7@h.one - 윤은우) - 15일 중 2일 사용
        balances.add(MemberBalance.builder()
                .memberId(DEV_MEMBER2_ID)
                .companyId(COMPANY_ID)
                .year(currentYear)
                .balanceTypeCode(annualLeaveType.getTypeCode())
                .totalGranted(15.0)
                .totalUsed(2.0)
                .remaining(13.0)
                .expirationDate(LocalDate.of(currentYear, 12, 31))
                .isPaid(true)
                .build());

        memberBalanceRepository.saveAll(balances);
    }

    /**
     * 테스트용 디바이스 등록 신청 생성
     * 다양한 상태의 디바이스 등록 신청을 생성하여 승인 프로세스 테스트 가능하도록 함
     */
    private void createSampleDeviceRequests() {
        List<Request> requests = new ArrayList<>();

        // 1. 인사팀 직원1 (이서준) - APPROVED 상태 (노트북)
        requests.add(Request.builder()
                .memberId(HR_MEMBER1_ID)
                .deviceId("DEVICE-LAPTOP-HR001")
                .deviceName("이서준 MacBook Pro")
                .deviceType(DeviceType.LAPTOP)
                .reason("업무용 노트북 등록")
                .status(RequestStatus.APPROVED)
                .policy(null)
                .requestUnit(null)
                .startDateTime(null)
                .endDateTime(null)
                .deductionDays(null)
                .build());

        // 2. 인사팀 직원2 (박도윤) - PENDING 상태 (모바일)
        requests.add(Request.builder()
                .memberId(HR_MEMBER2_ID)
                .deviceId("DEVICE-MOBILE-HR002")
                .deviceName("박도윤 iPhone 15")
                .deviceType(DeviceType.MOBILE)
                .reason("모바일 출퇴근 체크용")
                .status(RequestStatus.PENDING)
                .policy(null)
                .requestUnit(null)
                .startDateTime(null)
                .endDateTime(null)
                .deductionDays(null)
                .build());

        // 3. 개발팀 직원1 (강지호) - APPROVED 상태 (노트북)
        requests.add(Request.builder()
                .memberId(DEV_MEMBER1_ID)
                .deviceId("DEVICE-LAPTOP-DEV001")
                .deviceName("강지호 LG Gram")
                .deviceType(DeviceType.LAPTOP)
                .reason("개발 업무용 노트북")
                .status(RequestStatus.APPROVED)
                .policy(null)
                .requestUnit(null)
                .startDateTime(null)
                .endDateTime(null)
                .deductionDays(null)
                .build());

        // 4. 개발팀 직원2 (윤은우) - PENDING 상태 (모바일)
        requests.add(Request.builder()
                .memberId(DEV_MEMBER2_ID)
                .deviceId("DEVICE-MOBILE-DEV002")
                .deviceName("윤은우 Galaxy S24")
                .deviceType(DeviceType.MOBILE)
                .reason("재택근무 시 출퇴근 기록용")
                .status(RequestStatus.PENDING)
                .policy(null)
                .requestUnit(null)
                .startDateTime(null)
                .endDateTime(null)
                .deductionDays(null)
                .build());

        // 5. 개발팀 직원1 (강지호) - PENDING 상태 (모바일 추가)
        requests.add(Request.builder()
                .memberId(DEV_MEMBER1_ID)
                .deviceId("DEVICE-MOBILE-DEV001")
                .deviceName("강지호 iPhone 14")
                .deviceType(DeviceType.MOBILE)
                .reason("개인 휴대폰 등록 요청")
                .status(RequestStatus.PENDING)
                .policy(null)
                .requestUnit(null)
                .startDateTime(null)
                .endDateTime(null)
                .deductionDays(null)
                .build());

        requestRepository.saveAll(requests);
    }
}
