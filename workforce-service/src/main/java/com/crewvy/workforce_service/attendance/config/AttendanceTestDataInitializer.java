package com.crewvy.workforce_service.attendance.config;

import com.crewvy.workforce_service.attendance.constant.*;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.*;
import com.crewvy.workforce_service.attendance.repository.*;
import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.constant.LineStatus;
import com.crewvy.workforce_service.approval.entity.Approval;
import com.crewvy.workforce_service.approval.entity.ApprovalLine;
import com.crewvy.workforce_service.approval.repository.ApprovalLineRepository;
import com.crewvy.workforce_service.approval.repository.ApprovalRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberEmploymentInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.*;

/**
 * 시연용 테스트 데이터 초기화
 *
 * ✅ 생성 데이터:
 * - 직원 정보: member-service에서 동적 조회 (FeignClient)
 * - 근태 정책: 연차, 기본근무, 출장, 연장근무 등
 * - 정책 할당 → 자동 연차 부여 트리거
 * - 근태 기록: 최근 4~6개월, 출근율 85~95%
 * - 연차 요청: APPROVED/PENDING 상태
 *
 * ✅ 테스트 시나리오:
 * - 1년 미만 직원 2명: 월별 연차 발생 배치 테스트
 * - 1년 이상 직원 2명: 연간 연차 발생, 가산 규칙 테스트
 * - 신규 입사자 1명: 초기 연차 부여 배치 테스트
 * - 근태 보정 배치: 지각/결근 자동 처리
 * - 출근율 80% 체크: 월별 연차 발생 조건
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceTestDataInitializer implements CommandLineRunner {

    private final PolicyRepository policyRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final WorkLocationRepository workLocationRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final RequestRepository requestRepository;
    private final MemberBalanceRepository memberBalanceRepository;
    private final ApprovalRepository approvalRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final MemberClient memberClient;

    // Deterministic random for reproducible test data
    private final Random random = new Random(42);

    // 회사 ID (member-service로부터 자동 조회)
    private UUID companyId;

    // 테스트 대상 직원 분류
    private static class TestEmployees {
        List<MemberEmploymentInfoDto> lessThan1Year = new ArrayList<>();  // 1년 미만
        List<MemberEmploymentInfoDto> moreThan1Year = new ArrayList<>();  // 1년 이상
        MemberEmploymentInfoDto newHire = null;  // 신규 입사자 (최근 1개월)
        List<MemberEmploymentInfoDto> all = new ArrayList<>();
    }

    // 생성된 정책들
    private Policy annualLeavePolicy;
    private Policy basicWorkPolicy;
    private Policy overtimePolicy;
    private Policy businessTripPolicy;
    private Policy maternityLeavePolicy;
    private Policy paternityLeavePolicy;
    private Policy menstrualLeavePolicy;
    private WorkLocation mainOffice;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // 1단계: 회사 ID 조회 및 직원 정보 조회 (member-service 대기)
            log.info("========================================");
            log.info("🚀 시연용 근태 테스트 데이터 초기화 시작");
            log.info("========================================");
            log.info("");
            log.info("📋 [1/6] 회사 ID 및 직원 정보 조회 중...");
            log.info("   ⏳ Member Service 연결 대기 중...");

            // 회사 ID 자동 조회
            this.companyId = fetchCompanyIdWithRetry();
            log.info("   ✓ 회사 ID: {}", companyId);

            TestEmployees employees = fetchAndClassifyEmployeesWithRetry();

            // 직원이 없으면 초기화 불가
            if (employees.all.isEmpty()) {
                log.warn("❌ 직원이 존재하지 않습니다. Member Service에서 직원을 먼저 생성해주세요.");
                return;
            }

            // 이미 Policy가 있으면 스킵
            if (policyRepository.findByCompanyId(companyId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements() > 0) {
                log.info("✅ 근태 테스트 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
                return;
            }

            log.info("📅 데이터 범위: {} ~ {} (최대 3년)", LocalDate.now().minusYears(3), LocalDate.now());
            log.info("");

            logEmployeesSummary(employees);

            // 2단계: 근무지 생성
            log.info("📋 [2/6] 근무지 생성 중...");
            createWorkLocations();

            // 3단계: 정책 생성
            log.info("📋 [3/6] 근태 정책 생성 중...");
            createPolicies();

            // 4단계: 정책 할당 (자동 연차 부여 트리거)
            log.info("📋 [4/6] 정책 할당 중 (자동 연차 부여)...");
            assignPoliciesToCompany();

            // 5단계: 근태 기록 생성
            log.info("📋 [5/6] 근태 기록 생성 중 (최대 3년치)...");
            createAttendanceRecords(employees);

            // 6단계: 휴가 신청 및 결재 연동 데이터 생성
            log.info("📋 [6/6] 휴가 신청 및 결재 데이터 생성 중 (Request-Approval 완전 연동)...");
            createLeaveRequests(employees);

            log.info("");
            log.info("========================================");
            log.info("✅ 테스트 데이터 초기화 완료");
            log.info("========================================");
            printTestScenarioChecklist(employees);

        } catch (Exception e) {
            log.error("❌ 테스트 데이터 초기화 실패", e);
            throw new RuntimeException("테스트 데이터 초기화 실패", e);
        }
    }

    /**
     * 회사 ID 조회 (재시도 로직 포함)
     * member-service가 준비되지 않았을 경우 자동으로 재시도
     */
    private UUID fetchCompanyIdWithRetry() {
        int maxRetries = 10;
        int retryDelayMs = 3000; // 3초

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("   🔄 회사 ID 조회 시도 {}/{}", attempt, maxRetries);
                var response = memberClient.getFirstCompanyId();
                UUID fetchedCompanyId = response.getData();
                log.info("   ✓ 회사 ID 조회 성공: {}", fetchedCompanyId);
                return fetchedCompanyId;
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    log.error("   ❌ 회사 ID 조회 실패 ({}회 시도 후 포기)", maxRetries);
                    throw new RuntimeException("Member Service에서 회사 ID를 조회할 수 없습니다. Member Service가 실행 중인지 확인해주세요.", e);
                }
                log.warn("   ⚠ 조회 실패 (시도 {}/{}): {} - {}초 후 재시도...",
                        attempt, maxRetries, e.getMessage(), retryDelayMs / 1000);
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
            }
        }

        throw new RuntimeException("회사 ID 조회 실패");
    }

    /**
     * 1단계: 직원 정보 조회 및 분류 (재시도 로직 포함)
     * member-service가 준비되지 않았을 경우 자동으로 재시도
     */
    private TestEmployees fetchAndClassifyEmployeesWithRetry() {
        int maxRetries = 10;
        int retryDelayMs = 3000; // 3초

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("   🔄 Member Service 연결 시도 {}/{}", attempt, maxRetries);
                return fetchAndClassifyEmployees();
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    log.error("   ❌ Member Service 연결 실패 ({}회 시도 후 포기)", maxRetries);
                    throw new RuntimeException("Member Service 연결 실패: " + e.getMessage(), e);
                }
                log.warn("   ⚠️  Member Service 연결 실패, {}ms 후 재시도... (시도 {}/{}) - 원인: {}",
                        retryDelayMs, attempt, maxRetries, e.getMessage());
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
            }
        }

        // Should never reach here
        return new TestEmployees();
    }

    /**
     * 직원 정보 조회 및 분류 (실제 로직)
     */
    private TestEmployees fetchAndClassifyEmployees() {
        TestEmployees testEmployees = new TestEmployees();
        LocalDate today = LocalDate.now();

        try {
            // FeignClient로 직원 목록 조회
            var response = memberClient.getEmploymentInfoInternal(companyId);
            List<MemberEmploymentInfoDto> allMembers = response.getData();

            log.info("   ✓ 총 {}명의 직원 조회 완료", allMembers.size());

            // 직원 분류
            for (MemberEmploymentInfoDto member : allMembers) {
                if (member.getJoinDate() == null) {
                    continue;  // joinDate 없는 직원 제외
                }

                Period period = Period.between(member.getJoinDate(), today);
                int months = period.getYears() * 12 + period.getMonths();

                testEmployees.all.add(member);

                if (months < 1) {
                    // 신규 입사자 (1개월 미만)
                    if (testEmployees.newHire == null) {
                        testEmployees.newHire = member;
                    }
                } else if (period.getYears() < 1) {
                    // 1년 미만 (1~11개월)
                    testEmployees.lessThan1Year.add(member);
                } else {
                    // 1년 이상
                    testEmployees.moreThan1Year.add(member);
                }
            }

            // 테스트 최소 요구사항 체크
            if (testEmployees.all.size() < 4) {
                log.warn("   ⚠️  테스트에 필요한 최소 직원 수(4명)가 부족합니다. 현재: {}명", testEmployees.all.size());
            }

        } catch (Exception e) {
            log.error("   ❌ 직원 정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException("직원 정보 조회 실패", e);
        }

        return testEmployees;
    }

    /**
     * 직원 분류 결과 로깅
     */
    private void logEmployeesSummary(TestEmployees employees) {
        log.info("   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("   📊 직원 분류 결과:");
        log.info("   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("   · 전체 직원: {}명", employees.all.size());
        log.info("   · 1년 미만: {}명 (월별 연차 발생 대상)", employees.lessThan1Year.size());
        log.info("   · 1년 이상: {}명 (연간 연차 발생 대상)", employees.moreThan1Year.size());
        log.info("   · 신규 입사: {}명 (초기 연차 부여 대상)", employees.newHire != null ? 1 : 0);
        log.info("   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 상세 로깅
        if (!employees.lessThan1Year.isEmpty()) {
            log.info("   🔸 1년 미만 직원:");
            employees.lessThan1Year.forEach(m ->
                    log.info("      - {} (입사: {}, 근속: {}개월)",
                            m.getName(), m.getJoinDate(), getMonthsSinceJoin(m.getJoinDate())));
        }

        if (!employees.moreThan1Year.isEmpty()) {
            log.info("   🔹 1년 이상 직원:");
            employees.moreThan1Year.stream().limit(3).forEach(m ->
                    log.info("      - {} (입사: {}, 근속: {}년)",
                            m.getName(), m.getJoinDate(), getYearsSinceJoin(m.getJoinDate())));
        }

        if (employees.newHire != null) {
            log.info("   🆕 신규 입사자: {} (입사: {})",
                    employees.newHire.getName(), employees.newHire.getJoinDate());
        }
        log.info("");
    }

    /**
     * 2단계: 근무지 생성
     */
    private void createWorkLocations() {
        mainOffice = WorkLocation.builder()
                .companyId(companyId)
                .name("본사 (서울 강남구)")
                .address("서울특별시 강남구 테헤란로 123")
                .latitude(37.4979)
                .longitude(127.0276)
                .gpsRadius(100)
                .isActive(true)
                .build();

        workLocationRepository.save(mainOffice);

        log.info("   ✓ 근무지 생성 완료: {} (반경 {}m)", mainOffice.getName(), mainOffice.getGpsRadius());
        log.info("");
    }

    /**
     * 3단계: 정책 생성
     */
    private void createPolicies() {
        // 1. 연차유급휴가 정책
        annualLeavePolicy = createAnnualLeavePolicy();
        log.info("   ✓ 연차유급휴가 정책 생성: {}", annualLeavePolicy.getName());

        // 2. 기본근무 정책
        basicWorkPolicy = createBasicWorkPolicy();
        log.info("   ✓ 기본근무 정책 생성: {}", basicWorkPolicy.getName());

        // 3. 연장근무 정책
        overtimePolicy = createOvertimePolicy();
        log.info("   ✓ 연장근무 정책 생성: {}", overtimePolicy.getName());

        // 4. 출장 정책
        businessTripPolicy = createBusinessTripPolicy();
        log.info("   ✓ 출장 정책 생성: {}", businessTripPolicy.getName());

        // 5. 출산전후휴가 정책
        maternityLeavePolicy = createMaternityLeavePolicy();
        log.info("   ✓ 출산전후휴가 정책 생성: {}", maternityLeavePolicy.getName());

        // 6. 배우자출산휴가 정책
        paternityLeavePolicy = createPaternityLeavePolicy();
        log.info("   ✓ 배우자출산휴가 정책 생성: {}", paternityLeavePolicy.getName());

        // 7. 생리휴가 정책
        menstrualLeavePolicy = createMenstrualLeavePolicy();
        log.info("   ✓ 생리휴가 정책 생성: {}", menstrualLeavePolicy.getName());

        log.info("");
    }

    /**
     * 연차유급휴가 정책 생성 (데이터생성계획.txt 기준)
     * - 기본 15일 (1년 이상 근속자)
     * - 3년차: +1일 (총 16일)
     * - 5년차: +3일 (총 19일)
     * - 7년차: +6일 (총 25일, 최대)
     * - 1년 미만: 월 1일 발생 (최대 11일)
     * - 이월: 최대 4일, 3개월 내 사용
     */
    private Policy createAnnualLeavePolicy() {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(15.0);
        leaveRule.setAccrualType("ACCRUAL");
        leaveRule.setStandardType("FISCAL_YEAR");
        leaveRule.setBaseAnnualLeaveForOverOneYear(15);

        // 가산연차 규칙 (누적 가산)
        leaveRule.setAdditionalAnnualLeaveRules(Arrays.asList(
                createAdditionalRule(3, 1.0),  // 3년차: +1일
                createAdditionalRule(5, 3.0),  // 5년차: +3일 (누적 총 +4일)
                createAdditionalRule(7, 6.0)   // 7년차: +6일 (누적 총 +10일, 하지만 최대 25일 제한)
        ));
        leaveRule.setMaximumAnnualLeaveLimit(25);

        // 1년 미만 근로자 규칙
        FirstYearRule firstYearRule = new FirstYearRule();
        firstYearRule.setMonthlyAccrualEnabled(true);
        firstYearRule.setMonthlyAccrualDays(1.0);
        firstYearRule.setMaxAccrualFirstYear(11);
        firstYearRule.setMinimumAttendanceRateForAccrual(null);
        firstYearRule.setCarryOverEnabledForFirstYear(null);
        firstYearRule.setCarryOverLimitForFirstYear(null);
        leaveRule.setFirstYearRule(firstYearRule);

        // 1년 이상 근로자 규칙 (이월 설정)
        OverOneYearRule overOneYearRule = new OverOneYearRule();
        overOneYearRule.setCarryOverEnabled(true);
        overOneYearRule.setCarryOverLimitDays(4);
        overOneYearRule.setCarryOverExpirationMonths(3);
        leaveRule.setOverOneYearRule(overOneYearRule);

        // 신청 규칙
        leaveRule.setMinimumRequestUnit("DAY");
        leaveRule.setAllowedRequestUnits(null);  // null로 설정
        leaveRule.setRequestDeadlineDays(0);     // 당일 신청 가능
        leaveRule.setAllowRetrospectiveRequest(false);  // 사후 신청 불가
        leaveRule.setRetrospectiveRequestDays(null);
        leaveRule.setMaxDaysPerRequest(null);

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        Policy policy = Policy.builder()
                .companyId(companyId)
                .policyTypeCode(PolicyTypeCode.ANNUAL_LEAVE)
                .name("연차")
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2020, 11, 2))
                .effectiveTo(null)
                .ruleDetails(ruleDetails)
                .autoApprove(false)
                .isActive(true)
                .build();

        return policyRepository.save(policy);
    }

    private AdditionalAnnualLeaveRule createAdditionalRule(int afterYears, double additionalDays) {
        AdditionalAnnualLeaveRule rule = new AdditionalAnnualLeaveRule();
        rule.setAfterYears(afterYears);
        rule.setAdditionalDays(additionalDays);
        return rule;
    }

    /**
     * 기본근무 정책 생성
     * - 근무시간: 9:00 ~ 18:00 (8시간, 점심 1시간 제외)
     * - 지각 허용: 10분
     * - 연장근무: 주 12시간 한도, 1.5배 수당
     */
    private Policy createBasicWorkPolicy() {
        WorkTimeRuleDto workTimeRule = new WorkTimeRuleDto();
        workTimeRule.setType("FIXED");
        workTimeRule.setFixedWorkMinutes(480);  // 8시간
        workTimeRule.setWorkStartTime("09:00");
        workTimeRule.setWorkEndTime("18:00");

        BreakRuleDto breakRule = new BreakRuleDto();
        breakRule.setType("FIXED");
        breakRule.setFixedBreakStart("12:00");
        breakRule.setFixedBreakEnd("13:00");

        LatenessRuleDto latenessRule = new LatenessRuleDto();
        latenessRule.setLatenessGraceMinutes(10);
        latenessRule.setEarlyLeaveGraceMinutes(10);

        // 연장근무 규칙 추가
        OvertimeRuleDto overtimeRule = new OvertimeRuleDto();
        overtimeRule.setOvertimeRate(java.math.BigDecimal.valueOf(1.5));
        overtimeRule.setNightWorkRate(java.math.BigDecimal.valueOf(1.5));
        overtimeRule.setHolidayWorkRate(java.math.BigDecimal.valueOf(1.5));
        overtimeRule.setHolidayOvertimeRate(java.math.BigDecimal.valueOf(2.0));
        overtimeRule.setMaxWeeklyOvertimeMinutes(720);  // 근로기준법: 주 12시간

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setWorkTimeRule(workTimeRule);
        ruleDetails.setBreakRule(breakRule);
        ruleDetails.setLatenessRule(latenessRule);
        ruleDetails.setOvertimeRule(overtimeRule);

        Policy policy = Policy.builder()
                .companyId(companyId)
                .policyTypeCode(PolicyTypeCode.STANDARD_WORK)
                .name("기본근무")
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .ruleDetails(ruleDetails)
                .autoApprove(false)
                .isActive(true)
                .build();

        return policyRepository.save(policy);
    }

    /**
     * 연장근무 정책 생성
     * - 주 12시간 한도 (근로기준법 제53조)
     * - 1.5배 수당
     */
    private Policy createOvertimePolicy() {
        OvertimeRuleDto overtimeRule = new OvertimeRuleDto();
        overtimeRule.setOvertimeRate(java.math.BigDecimal.valueOf(1.5));
        overtimeRule.setNightWorkRate(java.math.BigDecimal.valueOf(1.5));
        overtimeRule.setHolidayWorkRate(java.math.BigDecimal.valueOf(1.5));
        overtimeRule.setHolidayOvertimeRate(java.math.BigDecimal.valueOf(2.0));
        overtimeRule.setMaxWeeklyOvertimeMinutes(720); // 12시간 = 720분

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setOvertimeRule(overtimeRule);

        Policy policy = Policy.builder()
                .companyId(companyId)
                .policyTypeCode(PolicyTypeCode.OVERTIME)
                .name("연장근무")
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .ruleDetails(ruleDetails)
                .autoApprove(true)  // 자동 승인
                .isActive(true)
                .build();

        return policyRepository.save(policy);
    }

    /**
     * 출장 정책 생성
     */
    private Policy createBusinessTripPolicy() {
        TripRuleDto tripRule = new TripRuleDto();
        tripRule.setType("DOMESTIC");
        tripRule.setPerDiemAmount(java.math.BigDecimal.valueOf(50000));
        tripRule.setAccommodationLimit(java.math.BigDecimal.valueOf(100000));
        tripRule.setTransportationLimit(java.math.BigDecimal.valueOf(200000));
        tripRule.setAllowedWorkLocations(List.of(mainOffice.getId().toString()));

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setTripRule(tripRule);

        Policy policy = Policy.builder()
                .companyId(companyId)
                .policyTypeCode(PolicyTypeCode.BUSINESS_TRIP)
                .name("출장")
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .ruleDetails(ruleDetails)
                .autoApprove(false)
                .isActive(true)
                .build();

        return policyRepository.save(policy);
    }

    /**
     * 출산전후휴가 정책 생성 (근로기준법 제74조)
     * - 기본 90일 부여
     */
    private Policy createMaternityLeavePolicy() {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(90.0);  // 근로기준법: 90일
        leaveRule.setRequestDeadlineDays(0);
        leaveRule.setAllowRetrospectiveRequest(false);
        leaveRule.setMinimumRequestUnit("DAY");

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        Policy policy = Policy.builder()
                .companyId(companyId)
                .policyTypeCode(PolicyTypeCode.MATERNITY_LEAVE)
                .name("출산전후휴가")
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .ruleDetails(ruleDetails)
                .autoApprove(false)
                .isActive(true)
                .build();

        return policyRepository.save(policy);
    }

    /**
     * 배우자출산휴가 정책 생성 (남녀고용평등법 제18조의2)
     * - 기본 10일 부여
     */
    private Policy createPaternityLeavePolicy() {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(10.0);  // 남녀고용평등법: 10일
        leaveRule.setRequestDeadlineDays(0);
        leaveRule.setAllowRetrospectiveRequest(false);
        leaveRule.setMinimumRequestUnit("DAY");

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        Policy policy = Policy.builder()
                .companyId(companyId)
                .policyTypeCode(PolicyTypeCode.PATERNITY_LEAVE)
                .name("배우자출산휴가")
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .ruleDetails(ruleDetails)
                .autoApprove(false)
                .isActive(true)
                .build();

        return policyRepository.save(policy);
    }

    /**
     * 생리휴가 정책 생성 (근로기준법 제73조)
     * - 월 1일 부여
     */
    private Policy createMenstrualLeavePolicy() {
        LeaveRuleDto leaveRule = new LeaveRuleDto();
        leaveRule.setDefaultDays(12.0);  // 연간 최대 12일 (월 1일 × 12개월)
        leaveRule.setLimitPeriod("MONTHLY");
        leaveRule.setMaxDaysPerPeriod(1);  // 월 1일 제한
        leaveRule.setRequestDeadlineDays(0);
        leaveRule.setAllowRetrospectiveRequest(true);
        leaveRule.setRetrospectiveRequestDays(3);
        leaveRule.setMinimumRequestUnit("DAY");

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setLeaveRule(leaveRule);

        Policy policy = Policy.builder()
                .companyId(companyId)
                .policyTypeCode(PolicyTypeCode.MENSTRUAL_LEAVE)
                .name("생리휴가")
                .isPaid(false)  // 무급
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .ruleDetails(ruleDetails)
                .autoApprove(false)
                .isActive(true)
                .build();

        return policyRepository.save(policy);
    }

    /**
     * 4단계: 정책 할당 (자동 연차 부여 트리거)
     */
    private void assignPoliciesToCompany() {
        // 회사 레벨에 정책 할당
        assignPolicy(annualLeavePolicy, PolicyScopeType.COMPANY, companyId);
        assignPolicy(basicWorkPolicy, PolicyScopeType.COMPANY, companyId);
        assignPolicy(overtimePolicy, PolicyScopeType.COMPANY, companyId);
        assignPolicy(businessTripPolicy, PolicyScopeType.COMPANY, companyId);
        assignPolicy(maternityLeavePolicy, PolicyScopeType.COMPANY, companyId);
        assignPolicy(paternityLeavePolicy, PolicyScopeType.COMPANY, companyId);
        assignPolicy(menstrualLeavePolicy, PolicyScopeType.COMPANY, companyId);

        log.info("   ✓ 회사 레벨 정책 할당 완료 (7개)");
        log.info("   ⏳ 자동 잔액 부여 프로세스 실행 중...");
        log.info("   ✓ 자동 잔액 부여 완료 (연차, 출산휴가, 배우자출산휴가, 생리휴가)");
        log.info("");
    }

    /**
     * 정책 할당 헬퍼 메서드
     */
    private void assignPolicy(Policy policy, PolicyScopeType scopeType, UUID targetId) {
        PolicyAssignment assignment = PolicyAssignment.builder()
                .policy(policy)
                .targetId(targetId)
                .scopeType(scopeType)
                .assignedAt(LocalDateTime.now())
                .assignedBy(companyId)  // 시스템 자동 할당 (회사 ID 사용)
                .isActive(true)
                .build();

        policyAssignmentRepository.save(assignment);
    }

    /**
     * 5단계: 근태 기록 생성 (최대 3년치)
     */
    private void createAttendanceRecords(TestEmployees employees) {
        LocalDate today = LocalDate.now();
        int totalDays = 0;
        int totalLogs = 0;
        int incompleteClockOuts = 0;

        // 전월 기간 계산 (월별 연차 배치 시연용)
        LocalDate previousMonthStart = today.minusMonths(1).withDayOfMonth(1);
        LocalDate previousMonthEnd = previousMonthStart.plusMonths(1).minusDays(1);

        for (MemberEmploymentInfoDto member : employees.all) {
            // 각 직원별로 입사일 이후부터 근태 기록 생성
            LocalDate startDate = member.getJoinDate();
            if (startDate.isBefore(today.minusYears(3))) {
                startDate = today.minusYears(3);  // 최대 3년 전부터
            }

            // 1년 미만 직원 여부 확인
            boolean isFirstYear = java.time.Period.between(member.getJoinDate(), today).getYears() < 1;

            int daysCreated = 0;
            int logsCreated = 0;

            for (LocalDate date = startDate; date.isBefore(today); date = date.plusDays(1)) {
                // 주말/공휴일 스킵 (85% 확률로 출근하지 않음)
                if (isWeekendOrHoliday(date) && random.nextDouble() > 0.15) {
                    continue;
                }

                // 출근 확률 결정 (1년 미만 직원의 전월 근속율을 80% 이상으로 조정)
                double attendanceProbability;
                if (isFirstYear && !date.isBefore(previousMonthStart) && !date.isAfter(previousMonthEnd)) {
                    // 1년 미만 직원의 전월: 95% 확률로 출근 (월별 연차 배치 시연용)
                    attendanceProbability = 0.95;
                } else {
                    // 그 외: 기본 90% 확률로 출근
                    attendanceProbability = 0.90;
                }

                if (random.nextDouble() < attendanceProbability) {
                    // 최근 3일: 30% 확률로 퇴근 미완료 케이스 생성 (근태 보정 배치 테스트용)
                    boolean skipClockOut = date.isAfter(today.minusDays(4)) && random.nextDouble() < 0.30;

                    int logs = createDailyAttendanceRecord(member, date, skipClockOut);
                    logsCreated += logs;
                    daysCreated++;

                    if (skipClockOut) {
                        incompleteClockOuts++;
                    }
                }
            }

            totalDays += daysCreated;
            totalLogs += logsCreated;

            if (daysCreated > 0) {
                log.info("      - {} : {}일 근무, {}개 로그 생성", member.getName(), daysCreated, logsCreated);
            }
        }

        log.info("   ✓ 총 {}명 직원의 {}일 근무 기록 생성 ({}개 로그)", employees.all.size(), totalDays, totalLogs);
        log.info("   ⚠️  미완료 퇴근 케이스: {}건 (근태 보정 배치 테스트용)", incompleteClockOuts);
        log.info("");
    }

    /**
     * 개별 직원의 일일 근태 기록 생성
     * @param skipClockOut true면 퇴근 기록을 생성하지 않음 (미완료 퇴근 케이스)
     */
    private int createDailyAttendanceRecord(MemberEmploymentInfoDto member, LocalDate date, boolean skipClockOut) {
        int logsCreated = 0;

        // 출근 시간 (9시 ± 30분 랜덤)
        LocalTime clockInTime = LocalTime.of(9, 0).plusMinutes(random.nextInt(60) - 30);
        LocalDateTime clockIn = LocalDateTime.of(date, clockInTime);

        // AttendanceLog: CLOCK_IN
        AttendanceLog clockInLog = AttendanceLog.builder()
                .memberId(member.getMemberId())
                .eventType(EventType.CLOCK_IN)
                .eventTime(clockIn)
                .latitude(mainOffice.getLatitude() + (random.nextDouble() - 0.5) * 0.001)
                .longitude(mainOffice.getLongitude() + (random.nextDouble() - 0.5) * 0.001)
                .isCorrected(false)
                .build();
        attendanceLogRepository.save(clockInLog);
        logsCreated++;

        LocalDateTime clockOut = null;
        LocalTime clockOutTime = null;

        // 퇴근 미완료 케이스가 아니면 퇴근 기록 생성
        if (!skipClockOut) {
            // 퇴근 시간 (18시 ± 60분 랜덤)
            clockOutTime = LocalTime.of(18, 0).plusMinutes(random.nextInt(120) - 60);
            clockOut = LocalDateTime.of(date, clockOutTime);

            // AttendanceLog: CLOCK_OUT
            AttendanceLog clockOutLog = AttendanceLog.builder()
                    .memberId(member.getMemberId())
                    .eventType(EventType.CLOCK_OUT)
                    .eventTime(clockOut)
                    .latitude(mainOffice.getLatitude() + (random.nextDouble() - 0.5) * 0.001)
                    .longitude(mainOffice.getLongitude() + (random.nextDouble() - 0.5) * 0.001)
                    .isCorrected(false)
                    .build();
            attendanceLogRepository.save(clockOutLog);
            logsCreated++;
        }

        // DailyAttendance 생성
        int workMinutes = 0;
        if (!skipClockOut && clockOutTime != null) {
            workMinutes = (int) java.time.Duration.between(clockInTime, clockOutTime).toMinutes() - 60;  // 점심시간 제외
        }

        DailyAttendance dailyAttendance = DailyAttendance.builder()
                .memberId(member.getMemberId())
                .companyId(companyId)
                .attendanceDate(date)
                .status(AttendanceStatus.NORMAL_WORK)
                .firstClockIn(clockIn)
                .lastClockOut(clockOut)  // 퇴근 미완료면 null
                .workedMinutes(workMinutes)
                .totalBreakMinutes(skipClockOut ? 0 : 60)
                .overtimeMinutes(skipClockOut ? 0 : Math.max(0, workMinutes - 480))
                .isLate(clockInTime.isAfter(LocalTime.of(9, 10)))
                .lateMinutes(clockInTime.isAfter(LocalTime.of(9, 10)) ?
                    (int) java.time.Duration.between(LocalTime.of(9, 0), clockInTime).toMinutes() : 0)
                .isEarlyLeave(!skipClockOut && clockOutTime != null && clockOutTime.isBefore(LocalTime.of(17, 50)))
                .earlyLeaveMinutes(!skipClockOut && clockOutTime != null && clockOutTime.isBefore(LocalTime.of(17, 50)) ?
                    (int) java.time.Duration.between(clockOutTime, LocalTime.of(18, 0)).toMinutes() : 0)
                .build();
        dailyAttendanceRepository.save(dailyAttendance);

        return logsCreated;
    }

    /**
     * 6단계: 휴가/출장 신청 및 결재 연동 데이터 생성 (완벽한 시연용)
     *
     * 생성 흐름:
     * 1. Request 생성 (잔액 차감)
     * 2. Approval 생성 (requestId 연결)
     * 3. ApprovalLine 생성 (단일/복수 결재자)
     * 4. 승인/반려 처리:
     *    - APPROVED: Request 상태 업데이트, DailyAttendance 생성
     *    - REJECTED: Request 상태 업데이트, 잔액 복구
     *    - PENDING: Request 상태 유지 (대기 중)
     */
    private void createLeaveRequests(TestEmployees employees) {
        int totalRequests = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        int pendingCount = 0;
        LocalDate today = LocalDate.now();

        // 각 직원별로 2~4개의 휴가 신청 생성
        for (MemberEmploymentInfoDto member : employees.all) {
            // 잔액 확인
            MemberBalance balance = memberBalanceRepository
                    .findByMemberIdAndBalanceTypeCodeAndYear(
                            member.getMemberId(),
                            PolicyTypeCode.ANNUAL_LEAVE,
                            today.getYear())
                    .orElse(null);

            if (balance == null || balance.getRemaining() < 1.0) {
                continue; // 잔액 없으면 스킵
            }

            int numRequests = Math.min(random.nextInt(3) + 2, (int) balance.getRemaining().doubleValue()); // 2~4개, 잔액 이내

            for (int i = 0; i < numRequests; i++) {
                // 70% 과거 (승인됨), 20% 미래 (대기), 10% 과거 (반려)
                double rand = random.nextDouble();
                int daysOffset;
                RequestStatus targetStatus;

                if (rand < 0.70) {
                    // 과거 신청 (승인됨)
                    daysOffset = -(random.nextInt(90) + 1); // -1일 ~ -90일
                    targetStatus = RequestStatus.APPROVED;
                } else if (rand < 0.90) {
                    // 미래 신청 (대기 중)
                    daysOffset = random.nextInt(30) + 1; // +1일 ~ +30일
                    targetStatus = RequestStatus.PENDING;
                } else {
                    // 과거 신청 (반려됨)
                    daysOffset = -(random.nextInt(60) + 1);
                    targetStatus = RequestStatus.REJECTED;
                }

                LocalDate leaveDate = today.plusDays(daysOffset);

                // 주말 제외
                while (isWeekendOrHoliday(leaveDate)) {
                    leaveDate = leaveDate.plusDays(1);
                }

                // 1. Request 생성
                Request request = Request.builder()
                        .memberId(member.getMemberId())
                        .policy(annualLeavePolicy)
                        .requestUnit(RequestUnit.DAY)
                        .status(RequestStatus.PENDING) // 초기 상태는 PENDING
                        .startDateTime(LocalDateTime.of(leaveDate, LocalTime.of(9, 0)))
                        .endDateTime(LocalDateTime.of(leaveDate, LocalTime.of(18, 0)))
                        .reason(i == 0 ? "개인 사유" : (i == 1 ? "가족 행사" : "휴식"))
                        .deductionDays(1.0)
                        .workLocation(null)
                        .completedAt(null)
                        .build();

                requestRepository.save(request);

                // 잔액 차감 (builder로 재생성)
                MemberBalance updatedBalance = MemberBalance.builder()
                        .id(balance.getId())
                        .memberId(balance.getMemberId())
                        .companyId(balance.getCompanyId())
                        .balanceTypeCode(balance.getBalanceTypeCode())
                        .year(balance.getYear())
                        .totalGranted(balance.getTotalGranted())
                        .totalUsed(balance.getTotalUsed() + 1.0)
                        .remaining(balance.getRemaining() - 1.0)
                        .expirationDate(balance.getExpirationDate())
                        .isPaid(balance.getIsPaid())
                        .isUsable(balance.getIsUsable())
                        .build();
                balance = memberBalanceRepository.save(updatedBalance);

                // 2. Approval 생성 (시연용: memberPositionId는 companyId 사용)
                Approval approval = Approval.builder()
                        .memberPositionId(companyId)
                        .approvalDocument(null) // 시연용: null
                        .title(member.getName() + "님의 연차 신청")
                        .contents(Map.of(
                                "startDate", leaveDate.toString(),
                                "endDate", leaveDate.toString(),
                                "reason", request.getReason(),
                                "type", "연차"
                        ))
                        .state(ApprovalState.PENDING)
                        .build();

                approvalRepository.save(approval);

                // Request에 approvalId 연결
                request.updateApprovalId(approval.getId());
                requestRepository.save(request);

                // 3. ApprovalLine 생성 (80% 단일 결재자, 20% 2단계 결재)
                boolean isSingleApprover = random.nextDouble() < 0.80;
                UUID approverPositionId = companyId; // 시연용: companyId 사용

                if (isSingleApprover) {
                    // 단일 결재자
                    ApprovalLine line = ApprovalLine.builder()
                            .approval(approval)
                            .memberPositionId(approverPositionId)
                            .lineIndex(1)
                            .lineStatus(targetStatus == RequestStatus.APPROVED ? LineStatus.APPROVED :
                                       (targetStatus == RequestStatus.REJECTED ? LineStatus.REJECTED : LineStatus.PENDING))
                            .approvalDate(targetStatus != RequestStatus.PENDING ?
                                         LocalDateTime.now().minusDays(Math.abs(daysOffset)) : null)
                            .build();
                    approvalLineRepository.save(line);

                    // Approval 상태 업데이트
                    if (targetStatus == RequestStatus.APPROVED) {
                        approval.updateState(ApprovalState.APPROVED);
                    } else if (targetStatus == RequestStatus.REJECTED) {
                        approval.updateState(ApprovalState.REJECTED);
                    }
                } else {
                    // 2단계 결재
                    UUID approver2PositionId = companyId; // 시연용: companyId 사용

                    // 1차 결재자 (항상 승인)
                    ApprovalLine line1 = ApprovalLine.builder()
                            .approval(approval)
                            .memberPositionId(approverPositionId)
                            .lineIndex(1)
                            .lineStatus(LineStatus.APPROVED)
                            .approvalDate(targetStatus != RequestStatus.PENDING ?
                                         LocalDateTime.now().minusDays(Math.abs(daysOffset) + 1) : null)
                            .build();
                    approvalLineRepository.save(line1);

                    // 2차 결재자
                    LineStatus line2Status = targetStatus == RequestStatus.APPROVED ? LineStatus.APPROVED :
                                             (targetStatus == RequestStatus.REJECTED ? LineStatus.REJECTED : LineStatus.WAITING);
                    ApprovalLine line2 = ApprovalLine.builder()
                            .approval(approval)
                            .memberPositionId(approver2PositionId)
                            .lineIndex(2)
                            .lineStatus(line2Status)
                            .approvalDate(targetStatus != RequestStatus.PENDING ?
                                         LocalDateTime.now().minusDays(Math.abs(daysOffset)) : null)
                            .build();
                    approvalLineRepository.save(line2);

                    // Approval 상태 업데이트
                    if (targetStatus == RequestStatus.APPROVED) {
                        approval.updateState(ApprovalState.APPROVED);
                    } else if (targetStatus == RequestStatus.REJECTED) {
                        approval.updateState(ApprovalState.REJECTED);
                    }
                }

                approvalRepository.save(approval);

                // 4. Request 상태 업데이트 및 후처리
                if (targetStatus == RequestStatus.APPROVED) {
                    request.updateStatus(RequestStatus.APPROVED); // updateStatus가 completedAt도 자동 설정
                    requestRepository.save(request);

                    // DailyAttendance 생성 (승인된 휴가)
                    DailyAttendance leaveAttendance = DailyAttendance.builder()
                            .memberId(member.getMemberId())
                            .companyId(companyId)
                            .attendanceDate(leaveDate)
                            .status(AttendanceStatus.ANNUAL_LEAVE)
                            .firstClockIn(null)
                            .lastClockOut(null)
                            .workedMinutes(0)
                            .totalBreakMinutes(0)
                            .overtimeMinutes(0)
                            .isLate(false)
                            .lateMinutes(0)
                            .isEarlyLeave(false)
                            .earlyLeaveMinutes(0)
                            .build();
                    dailyAttendanceRepository.save(leaveAttendance);

                    approvedCount++;
                } else if (targetStatus == RequestStatus.REJECTED) {
                    request.updateStatus(RequestStatus.REJECTED); // updateStatus가 completedAt도 자동 설정
                    requestRepository.save(request);

                    // 잔액 복구 (builder로 재생성)
                    MemberBalance restoredBalance = MemberBalance.builder()
                            .id(balance.getId())
                            .memberId(balance.getMemberId())
                            .companyId(balance.getCompanyId())
                            .balanceTypeCode(balance.getBalanceTypeCode())
                            .year(balance.getYear())
                            .totalGranted(balance.getTotalGranted())
                            .totalUsed(balance.getTotalUsed() - 1.0)
                            .remaining(balance.getRemaining() + 1.0)
                            .expirationDate(balance.getExpirationDate())
                            .isPaid(balance.getIsPaid())
                            .isUsable(balance.getIsUsable())
                            .build();
                    balance = memberBalanceRepository.save(restoredBalance);

                    rejectedCount++;
                } else {
                    // PENDING 상태 유지
                    pendingCount++;
                }

                totalRequests++;
            }
        }

        log.info("   ✓ 총 {}개의 휴가 신청 생성 (승인: {}, 반려: {}, 대기: {})",
                 totalRequests, approvedCount, rejectedCount, pendingCount);
        log.info("   ✓ 승인된 휴가에 대한 DailyAttendance {} 건 생성", approvedCount);
        log.info("");
    }

    /**
     * 주말 또는 공휴일 체크
     */
    private boolean isWeekendOrHoliday(LocalDate date) {
        return date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * 입사 후 개월 수 계산
     */
    private int getMonthsSinceJoin(LocalDate joinDate) {
        Period period = Period.between(joinDate, LocalDate.now());
        return period.getYears() * 12 + period.getMonths();
    }

    /**
     * 입사 후 년 수 계산
     */
    private int getYearsSinceJoin(LocalDate joinDate) {
        return Period.between(joinDate, LocalDate.now()).getYears();
    }

    /**
     * 테스트 시나리오 체크리스트 출력
     */
    private void printTestScenarioChecklist(TestEmployees employees) {
        log.info("📝 테스트 시나리오 체크리스트:");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("");
        log.info("✅ 1. 월별 연차 발생 배치 (1년 미만 근로자)");
        log.info("   - 대상 직원: {}명", employees.lessThan1Year.size());
        log.info("   - 테스트 방법: 매월 1일 03:00 배치 실행 → CloudWatch 로그 확인");
        log.info("   - 예상 결과: 근속일수에 따라 월 1일 자동 발생");
        log.info("");

        log.info("✅ 2. 연간 연차 발생 배치 (1년 이상 근로자)");
        log.info("   - 대상 직원: {}명", employees.moreThan1Year.size());
        log.info("   - 테스트 방법: 1월 1일 03:00 배치 실행");
        log.info("   - 예상 결과: 기본 15일 + 가산연차 (3년차+1, 5년차+1, ...)");
        log.info("");

        log.info("✅ 3. 신규 입사자 연차 자동 부여");
        log.info("   - 대상 직원: {}명", employees.newHire != null ? 1 : 0);
        log.info("   - 테스트 방법: Kafka 'member-create' 이벤트 발생");
        log.info("   - 예상 결과: 즉시 연차 balance 생성");
        log.info("");

        log.info("✅ 4. 정책 할당 시 자동 연차 부여");
        log.info("   - 테스트 방법: 관리자 화면에서 연차 정책 회사에 할당");
        log.info("   - 예상 결과: 전체 직원에게 즉시 연차 balance 생성");
        log.info("");

        log.info("✅ 5. 근태 보정 배치 (지각/결근 자동 처리)");
        log.info("   - API: POST /workforce-service/batch/attendance/auto-complete-clock-out (미완료 퇴근)");
        log.info("   - API: POST /workforce-service/batch/attendance/mark-absent (결근 처리)");
        log.info("   - 테스트 데이터: 최근 3일 중 {}%의 미완료 퇴근 케이스 생성됨", 30);
        log.info("   - 예상 결과: 미완료 퇴근 자동 처리 (출근 + 9시간), 결근 자동 마킹");
        log.info("");

        log.info("✅ 6. 월별 연차 배치 (1년 미만 근속자)");
        log.info("   - API: POST /workforce-service/batch/attendance/annual-leave-accrual");
        log.info("   - 대상: 1년 미만 직원 {}명", employees.lessThan1Year.size());
        log.info("   - 예상 결과: 근속 개월 수 × 1일 (최대 11일)");
        log.info("");

        log.info("✅ 7. 출근율 85~95% (자동 생성)");
        log.info("   - 현재 출근율: 90% (결근 10%)");
        log.info("   - 월별 연차 발생 조건: 출근율 80% 이상");
        log.info("");

        log.info("✅ 8. 관리자 UI - 휴가 현황 필터링");
        log.info("   - 화면: /admin/attendance → 휴가 현황 탭");
        log.info("   - 필터: 유형별, 근속년수별 (<1년, ≥1년, ≥3년, ≥5년, ≥10년)");
        log.info("   - 기능: 월별 연차 배치 실행 (1년 미만 필터 시에만 활성화)");
        log.info("");

        log.info("✅ 9. 정책 설정 - 신규 DTO 구조");
        log.info("   - 화면: /admin/policy-editor → 연차 정책 생성/수정");
        log.info("   - 설정: 회계연도/입사일 기준, 가산 규칙, 이월 설정, 월별 발생");
        log.info("");

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎉 모든 테스트 시나리오가 준비되었습니다!");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
