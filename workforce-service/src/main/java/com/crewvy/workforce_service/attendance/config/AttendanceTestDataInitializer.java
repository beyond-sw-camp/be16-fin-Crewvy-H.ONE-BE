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
    private static final UUID COMPANY_ID = UUID.fromString("042c11f9-ec9b-49ba-a32e-41723bbdc3e5");

    // 테스트용 멤버 ID들 (AutoCreateAdmin에서 생성된 emp1~10@h.one)
    private static final UUID HR_ADMIN_ID = UUID.fromString("f2deb0ff-d527-41fd-be25-d0f87eaa896e"); // emp1 - 김민준
    private static final UUID HR_MEMBER1_ID = UUID.fromString("b869ec13-4406-46c9-9588-45bf9e9cdff3"); // emp2 - 이서준
    private static final UUID HR_MEMBER2_ID = UUID.fromString("b2e42207-fa34-46b1-af3f-3846e723aee0"); // emp3 - 박도윤
    private static final UUID HR_MEMBER3_ID = UUID.fromString("bbf598ff-6f69-44c7-aacf-41c253da302e"); // emp4 - 최시우
    private static final UUID HR_MEMBER4_ID = UUID.fromString("f2c1b71b-c8a2-4bb0-97b2-7a40fe85e50c"); // emp5 - 정하준
    private static final UUID DEV_MEMBER1_ID = UUID.fromString("4a07c318-b980-49f2-8514-0cc98c6e312a"); // emp6 - 강지호
    private static final UUID DEV_MEMBER2_ID = UUID.fromString("c55c16c9-4551-4f63-a846-26e23fb49d3a"); // emp7 - 윤은우
    private static final UUID DEV_MEMBER3_ID = UUID.fromString("06908a23-31fc-4355-a44d-82602c3f96ea"); // emp8 - 임선우
    private static final UUID DEV_MEMBER4_ID = UUID.fromString("dfa12591-a3ea-4b92-8daf-a3d0ef315861"); // emp9 - 한유찬
    private static final UUID DEV_MEMBER5_ID = UUID.fromString("be7cd1ed-4457-49d5-bcf1-e2e0b79d564d"); // emp10 - 오이안

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
        log.info("📊 생성된 데이터: 근태기록 {}건, 신청 {}건",
                dailyAttendanceRepository.count(), requestRepository.count());
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
                // 휴가
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.ANNUAL_LEAVE).typeName("연차유급휴가").balanceDeductible(true).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),
                PolicyType.builder().companyId(COMPANY_ID).typeCode(PolicyTypeCode.CHILDCARE_LEAVE).typeName("육아휴직").balanceDeductible(false).categoryCode(PolicyCategory.ABSENCE).priority(1).build(),

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

        // ========== 육아휴직 정책 ==========
        policies.add(Policy.builder()
                .policyType(policyTypes.get(PolicyTypeCode.CHILDCARE_LEAVE))
                .companyId(COMPANY_ID)
                .name("육아휴직 정책")
                .ruleDetails(new PolicyRuleDetails())
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
     * 전 직원에게 정책 할당 (회사 레벨)
     */
    private void assignPoliciesToAllMembers(Map<PolicyTypeCode, Policy> policies) {
        List<PolicyAssignment> assignments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Policy policy : policies.values()) {
            assignments.add(PolicyAssignment.builder()
                    .policy(policy)
                    .scopeType(PolicyScopeType.COMPANY)
                    .targetId(COMPANY_ID)
                    .assignedBy(HR_ADMIN_ID)
                    .assignedAt(now)
                    .isActive(true)
                    .build());
        }

        policyAssignmentRepository.saveAll(assignments);
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

        // 5. 육아휴직 (HR_MEMBER3만, 1개월간)
        LocalDate childcareStart = LocalDate.now().minusDays(45);
        LocalDate childcareEnd = LocalDate.now().minusDays(15);
        requests.add(Request.builder()
                .memberId(HR_MEMBER3_ID)
                .policy(childcarePolicy)
                .requestUnit(RequestUnit.DAY)
                .startDateTime(childcareStart.atStartOfDay())
                .endDateTime(childcareEnd.atTime(23, 59))
                .deductionDays(0.0)
                .reason("육아휴직")
                .status(RequestStatus.APPROVED)
                .build());

        // 6. 미래 일정 (공유 캘린더용)
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
                .filter(r -> r.getPolicy().getPolicyType().getTypeCode() == PolicyTypeCode.ANNUAL_LEAVE ||
                             r.getPolicy().getPolicyType().getTypeCode() == PolicyTypeCode.CHILDCARE_LEAVE ||
                             r.getPolicy().getPolicyType().getTypeCode() == PolicyTypeCode.BUSINESS_TRIP)
                .collect(Collectors.toList());

        for (Request req : approvedLeaves) {
            LocalDate start = req.getStartDateTime().toLocalDate();
            LocalDate end = req.getEndDateTime().toLocalDate();

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
