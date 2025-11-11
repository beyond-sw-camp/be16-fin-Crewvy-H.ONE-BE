package com.crewvy.workforce_service.attendance.config;

import com.crewvy.workforce_service.attendance.constant.*;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.*;
import com.crewvy.workforce_service.attendance.repository.*;
import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.approval.constant.LineStatus;
import com.crewvy.workforce_service.approval.entity.Approval;
import com.crewvy.workforce_service.approval.entity.ApprovalDocument;
import com.crewvy.workforce_service.approval.entity.ApprovalLine;
import com.crewvy.workforce_service.approval.repository.ApprovalDocumentRepository;
import com.crewvy.workforce_service.approval.repository.ApprovalLineRepository;
import com.crewvy.workforce_service.approval.repository.ApprovalRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberEmploymentInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
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
 * - 근태 정책: 연차, 기본근무, 출장, 연장근무, 야간근무, 휴일근무 등
 * - 정책 할당 → 자동 연차 부여 트리거
 * - 근태 기록: 최근 3개월, 출근율 85~95%
 * - 다양한 신청: 연차/출장/연장근무/야간근무/휴일근무 (APPROVED/PENDING/REJECTED 상태)
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
@Order(10)  // ApprovalDocumentInitializer(@Order(1)) 이후에 실행
public class AttendanceTestDataInitializer implements ApplicationRunner {

    private final PolicyRepository policyRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final WorkLocationRepository workLocationRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final RequestRepository requestRepository;
    private final MemberBalanceRepository memberBalanceRepository;
    private final ApprovalRepository approvalRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final MemberClient memberClient;

    // 시연 기준일
    private static final LocalDate DEMO_DATE = LocalDate.of(2025, 11, 12);

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
    private Policy nightWorkPolicy;
    private Policy holidayWorkPolicy;
    private Policy businessTripPolicy;
    private Policy maternityLeavePolicy;
    private Policy paternityLeavePolicy;
    private Policy menstrualLeavePolicy;
    private WorkLocation mainOffice;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            // 1단계: 회사 ID 조회 및 직원 정보 조회 (member-service 대기)
            log.info("========================================");
            log.info("🚀 시연용 근태 테스트 데이터 초기화 시작");
            log.info("========================================");
            log.info("");
            log.info("📋 [1/6] 회사 ID 및 직원 정보 조회 중...");
            log.info("   ⏳ Member Service 연결 대기 중...");

            // 모든 회사 ID 조회
            List<UUID> companyIds = fetchAllCompanyIdsWithRetry();

            if (companyIds.isEmpty()) {
                log.warn("❌ 회사가 존재하지 않습니다. Member Service에서 회사를 먼저 생성해주세요.");
                return;
            }

            log.info("   ✓ 총 {}개 회사 발견", companyIds.size());
            log.info("");

            // 각 회사별로 테스트 데이터 생성
            for (int i = 0; i < companyIds.size(); i++) {
                UUID currentCompanyId = companyIds.get(i);
                log.info("========================================");
                log.info("🏢 회사 {}/{} 처리 중 (ID: {})", i + 1, companyIds.size(), currentCompanyId);
                log.info("========================================");
                log.info("");

                initializeCompanyData(currentCompanyId, i + 1, companyIds.size());
            }

            log.info("");
            log.info("========================================");
            log.info("✅ 모든 회사 테스트 데이터 초기화 완료");
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ 테스트 데이터 초기화 실패", e);
            throw new RuntimeException("테스트 데이터 초기화 실패", e);
        }
    }

    /**
     * 개별 회사 데이터 초기화
     */
    private void initializeCompanyData(UUID currentCompanyId, int companyIndex, int totalCompanies) {
        this.companyId = currentCompanyId;

        log.info("📋 [1/7] 직원 정보 조회 중...");
        TestEmployees employees = fetchAndClassifyEmployeesWithRetry();

        // 직원이 없으면 스킵
        if (employees.all.isEmpty()) {
            log.warn("⚠️  회사 {}에 직원이 존재하지 않습니다. 스킵합니다.", companyId);
            return;
        }

        // 이미 Policy가 있는지 체크
        boolean policiesExist = policyRepository.findByCompanyId(companyId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements() > 0;

        if (policiesExist) {
            // Policy는 있는데 PolicyAssignment가 없으면 재할당 필요
            long assignmentCount = policyAssignmentRepository.findAll()
                    .stream()
                    .filter(pa -> pa.getPolicy().getCompanyId().equals(companyId))
                    .count();

            if (assignmentCount == 0) {
                log.warn("⚠️  회사 {}의 정책은 존재하지만 정책 할당이 없습니다. 정책 재할당을 진행합니다.", companyId);

                // 정책 조회 및 할당
                annualLeavePolicy = policyRepository.findAll()
                        .stream()
                        .filter(p -> p.getCompanyId().equals(companyId) && p.getPolicyTypeCode() == PolicyTypeCode.ANNUAL_LEAVE)
                        .findFirst()
                        .orElse(null);
                basicWorkPolicy = policyRepository.findAll()
                        .stream()
                        .filter(p -> p.getCompanyId().equals(companyId) && p.getPolicyTypeCode() == PolicyTypeCode.STANDARD_WORK)
                        .findFirst()
                        .orElse(null);
                overtimePolicy = policyRepository.findAll()
                        .stream()
                        .filter(p -> p.getCompanyId().equals(companyId) && p.getPolicyTypeCode() == PolicyTypeCode.OVERTIME)
                        .findFirst()
                        .orElse(null);
                nightWorkPolicy = policyRepository.findAll()
                        .stream()
                        .filter(p -> p.getCompanyId().equals(companyId) && p.getPolicyTypeCode() == PolicyTypeCode.NIGHT_WORK)
                        .findFirst()
                        .orElse(null);
                holidayWorkPolicy = policyRepository.findAll()
                        .stream()
                        .filter(p -> p.getCompanyId().equals(companyId) && p.getPolicyTypeCode() == PolicyTypeCode.HOLIDAY_WORK)
                        .findFirst()
                        .orElse(null);
                businessTripPolicy = policyRepository.findAll()
                        .stream()
                        .filter(p -> p.getCompanyId().equals(companyId) && p.getPolicyTypeCode() == PolicyTypeCode.BUSINESS_TRIP)
                        .findFirst()
                        .orElse(null);
                maternityLeavePolicy = policyRepository.findAll()
                        .stream()
                        .filter(p -> p.getCompanyId().equals(companyId) && p.getPolicyTypeCode() == PolicyTypeCode.MATERNITY_LEAVE)
                        .findFirst()
                        .orElse(null);
                paternityLeavePolicy = policyRepository.findAll()
                        .stream()
                        .filter(p -> p.getCompanyId().equals(companyId) && p.getPolicyTypeCode() == PolicyTypeCode.PATERNITY_LEAVE)
                        .findFirst()
                        .orElse(null);
                menstrualLeavePolicy = policyRepository.findAll()
                        .stream()
                        .filter(p -> p.getCompanyId().equals(companyId) && p.getPolicyTypeCode() == PolicyTypeCode.MENSTRUAL_LEAVE)
                        .findFirst()
                        .orElse(null);

                // 정책 재할당 (자동 연차 부여 트리거)
                log.info("📋 정책 재할당 중 (자동 연차 부여)...");
                assignPoliciesToCompany();
                log.info("✅ 회사 {}의 정책 재할당 및 잔액 부여 완료", companyId);
                return;
            } else {
                log.info("✅ 회사 {}의 근태 테스트 데이터가 이미 존재합니다. 초기화를 건너뜁니다.", companyId);
                return;
            }
        }

        log.info("📅 시연 기준일: {}", DEMO_DATE);
        log.info("📅 데이터 범위: {} ~ {} (전월 전체 + 당월 현재까지)",
                DEMO_DATE.minusMonths(1).withDayOfMonth(1), DEMO_DATE.minusDays(1));
        log.info("");

        logEmployeesSummary(employees);

        // 2단계: 근무지 생성
        log.info("📋 [2/7] 근무지 생성 중...");
        createWorkLocations();

        // 3단계: 정책 생성
        log.info("📋 [3/7] 근태 정책 생성 중...");
        createPolicies();

        // 4단계: 정책 할당 (자동 연차 부여 트리거)
        log.info("📋 [4/7] 정책 할당 중 (자동 연차 부여)...");
        assignPoliciesToCompany();

        // 5단계: 근태 기록 생성 (전월+당월, 퇴근누락자 포함)
        log.info("📋 [5/7] 근태 기록 생성 중 (전월 전체 + 당월 전일까지, 퇴근누락자 포함)...");
        createAttendanceRecords(employees);

        // 6단계: 휴가 신청 및 결재 연동 데이터 생성
        log.info("📋 [6/8] 휴가 신청 데이터 생성 중 (Request-Approval 완전 연동)...");
        createLeaveRequests(employees);

        // 6-2단계: 출장 신청 데이터 생성
        log.info("📋 [6-2/8] 출장 신청 데이터 생성 중...");
        createTripRequests(employees);

        // 7단계: 추가근무 신청 및 DailyAttendance 연동 데이터 생성
        log.info("📋 [7/8] 추가근무 신청 데이터 생성 중 (연장/야간/휴일근무)...");
        createExtraWorkRequests(employees);

        log.info("");
        log.info("✅ 회사 {} 테스트 데이터 초기화 완료", companyId);
        log.info("");
        printTestScenarioChecklist(employees);
    }

    /**
     * 모든 회사 ID 조회 (재시도 로직 포함)
     * member-service가 준비되지 않았을 경우 자동으로 재시도
     */
    private List<UUID> fetchAllCompanyIdsWithRetry() {
        int maxRetries = 10;
        int retryDelayMs = 3000; // 3초

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("   🔄 회사 ID 조회 시도 {}/{}", attempt, maxRetries);
                var response = memberClient.getAllCompanyIds();
                List<UUID> companyIds = response.getData();
                log.info("   ✓ 회사 ID 조회 성공: {}개 회사", companyIds.size());
                return companyIds;
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

        // 4. 야간근무 정책
        nightWorkPolicy = createNightWorkPolicy();
        log.info("   ✓ 야간근무 정책 생성: {}", nightWorkPolicy.getName());

        // 5. 휴일근무 정책
        holidayWorkPolicy = createHolidayWorkPolicy();
        log.info("   ✓ 휴일근무 정책 생성: {}", holidayWorkPolicy.getName());

        // 6. 출장 정책
        businessTripPolicy = createBusinessTripPolicy();
        log.info("   ✓ 출장 정책 생성: {}", businessTripPolicy.getName());

        // 7. 출산전후휴가 정책
        maternityLeavePolicy = createMaternityLeavePolicy();
        log.info("   ✓ 출산전후휴가 정책 생성: {}", maternityLeavePolicy.getName());

        // 8. 배우자출산휴가 정책
        paternityLeavePolicy = createPaternityLeavePolicy();
        log.info("   ✓ 배우자출산휴가 정책 생성: {}", paternityLeavePolicy.getName());

        // 9. 생리휴가 정책
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
     * - GPS 인증: 본사 근무지 (100m 반경)
     */
    private Policy createBasicWorkPolicy() {
        WorkTimeRuleDto workTimeRule = new WorkTimeRuleDto();
        workTimeRule.setType("FIXED");
        workTimeRule.setWorkStartTime("09:00");
        workTimeRule.setWorkEndTime("18:00");
        // 총 근무시간 (휴게 제외): 9:00~18:00 = 9시간, 휴게 1시간 제외 = 8시간(480분)
        workTimeRule.setFixedWorkMinutes(480);

        AuthRuleDto authRule = new AuthRuleDto();
        authRule.setAllowedWorkLocationIds(List.of(mainOffice.getId()));
        authRule.setRequiredAuthTypes(List.of("GPS"));

        BreakRuleDto breakRule = new BreakRuleDto();
        breakRule.setType("FIXED");
        breakRule.setFixedBreakStart("12:00");
        breakRule.setFixedBreakEnd("13:00");

        LatenessRuleDto latenessRule = new LatenessRuleDto();
        latenessRule.setLatenessGraceMinutes(10);
        latenessRule.setEarlyLeaveGraceMinutes(10);

        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        ruleDetails.setWorkTimeRule(workTimeRule);
        ruleDetails.setAuthRule(authRule);
        ruleDetails.setBreakRule(breakRule);
        ruleDetails.setLatenessRule(latenessRule);

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
     * 야간근무 정책 생성
     * - 22시~06시 야간근무 (시간만 기록, 가산율은 급여서비스에서 계산)
     */
    private Policy createNightWorkPolicy() {
        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        // 야간근무는 별도 rule 없이 시간만 기록

        Policy policy = Policy.builder()
                .companyId(companyId)
                .policyTypeCode(PolicyTypeCode.NIGHT_WORK)
                .name("야간근무")
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .ruleDetails(ruleDetails)
                .autoApprove(true)
                .isActive(true)
                .build();

        return policyRepository.save(policy);
    }

    /**
     * 휴일근무 정책 생성
     * - 휴일(주말/공휴일)에 근무 (시간만 기록, 가산율은 급여서비스에서 계산)
     */
    private Policy createHolidayWorkPolicy() {
        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();
        // 휴일근무는 별도 rule 없이 시간만 기록

        Policy policy = Policy.builder()
                .companyId(companyId)
                .policyTypeCode(PolicyTypeCode.HOLIDAY_WORK)
                .name("휴일근무")
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .ruleDetails(ruleDetails)
                .autoApprove(true)
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
        tripRule.setAllowedWorkLocations(List.of(mainOffice.getName()));

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
     * 5단계: 근태 기록 생성 (결정적 패턴 기반)
     * - 기준일: DEMO_DATE (2025-11-12)
     * - 생성 범위: 전월 전체(2025-10) + 당월 현재까지(2025-11-01 ~ 2025-11-11)
     * - 퇴근 누락: DEMO_DATE.minusDays(1) = 2025-11-11에 CLOCK_IN만
     * - 출근율 계산용: 전월(10월) 평일 전체 데이터 필요
     */
    private void createAttendanceRecords(TestEmployees employees) {
        int totalDays = 0;
        int totalLogs = 0;
        int incompleteClockOuts = 0;

        // 전월 1일 ~ 당월 DEMO_DATE 전날까지
        LocalDate previousMonthStart = DEMO_DATE.minusMonths(1).withDayOfMonth(1); // 2025-10-01
        LocalDate currentMonthEnd = DEMO_DATE.minusDays(1); // 2025-11-11

        // 평일만 추출
        List<LocalDate> workDays = new ArrayList<>();
        for (LocalDate d = previousMonthStart; !d.isAfter(currentMonthEnd); d = d.plusDays(1)) {
            if (!isWeekendOrHoliday(d)) {
                workDays.add(d);
            }
        }

        LocalDate incompleteDateTarget = DEMO_DATE.minusDays(1); // 2025-11-11 (퇴근 누락일)

        int memberIndex = 0;
        for (MemberEmploymentInfoDto member : employees.all) {
            int daysCreated = 0;
            int logsCreated = 0;

            for (int i = 0; i < workDays.size(); i++) {
                LocalDate date = workDays.get(i);

                // 입사일 이전이면 스킵
                if (date.isBefore(member.getJoinDate())) {
                    continue;
                }

                // 퇴근 누락 여부: 2025-11-11에 memberIndex % 7 == 0인 직원만 퇴근 누락
                boolean skipClockOut = date.equals(incompleteDateTarget) && (memberIndex % 7 == 0);

                // 기본 근무 기록 생성 (정책 전달)
                int logs = createDailyAttendanceRecord(member, date, skipClockOut, i, memberIndex, basicWorkPolicy);
                logsCreated += logs;
                daysCreated++;

                if (skipClockOut) {
                    incompleteClockOuts++;
                }
            }

            memberIndex++;

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
     * 개별 직원의 일일 근태 기록 생성 (결정적 패턴)
     * @param skipClockOut true면 퇴근 기록을 생성하지 않음 (미완료 퇴근 케이스)
     * @param workDayIndex 근무일 인덱스 (0부터 시작, 패턴 결정용)
     * @param memberIndex 직원 인덱스 (패턴 결정용)
     * @param standardWorkPolicy 기본 근무 정책 (휴게시간 계산용)
     */
    private int createDailyAttendanceRecord(MemberEmploymentInfoDto member, LocalDate date, boolean skipClockOut, int workDayIndex, int memberIndex, Policy standardWorkPolicy) {
        int logsCreated = 0;

        // 정책에서 휴게시간 계산 (람다에서 사용하므로 final)
        int calculatedBreakMinutes = 60; // 기본값: 1시간

        if (standardWorkPolicy != null
                && standardWorkPolicy.getRuleDetails() != null
                && standardWorkPolicy.getRuleDetails().getBreakRule() != null) {
            var breakRule = standardWorkPolicy.getRuleDetails().getBreakRule();
            if ("FIXED".equals(breakRule.getType())
                    && breakRule.getFixedBreakStart() != null
                    && breakRule.getFixedBreakEnd() != null) {
                try {
                    LocalTime breakStart = LocalTime.parse(breakRule.getFixedBreakStart());
                    LocalTime breakEnd = LocalTime.parse(breakRule.getFixedBreakEnd());
                    calculatedBreakMinutes = (int) java.time.Duration.between(breakStart, breakEnd).toMinutes();
                } catch (Exception e) {
                    log.warn("휴게시간 파싱 실패, 기본값 60분 사용: {}", e.getMessage());
                    // calculatedBreakMinutes는 이미 60으로 초기화됨
                }
            }
        }

        final int breakMinutes = calculatedBreakMinutes;

        // 출근 시간: 08:30 ~ 09:15 사이에서 다양하게 생성
        // 정책: 09:00 기준, 지각 허용 10분
        // - 정상 출근: 08:30 ~ 09:00
        // - 지각: 09:01 ~ 09:15
        int clockInVariation = (memberIndex * 7 + workDayIndex * 3) % 46; // 0~45분 (46가지)
        int clockInHour = 8;
        int clockInMinute = 30 + clockInVariation; // 08:30 ~ 09:15
        if (clockInMinute >= 60) {
            clockInHour = 9;
            clockInMinute = clockInMinute - 60;
        }
        final LocalDateTime clockIn = LocalDateTime.of(date, LocalTime.of(clockInHour, clockInMinute));

        // AttendanceLog: CLOCK_IN
        AttendanceLog clockInLog = AttendanceLog.builder()
                .memberId(member.getMemberId())
                .eventType(EventType.CLOCK_IN)
                .eventTime(clockIn)
                .latitude(mainOffice.getLatitude())
                .longitude(mainOffice.getLongitude())
                .isCorrected(false)
                .build();
        attendanceLogRepository.save(clockInLog);
        logsCreated++;

        // 퇴근 시간 (미완료 케이스면 null)
        final LocalDateTime clockOut;
        if (!skipClockOut) {
            // 퇴근 시간: 17:45 ~ 18:30 사이에서 다양하게 생성
            // 정책: 18:00 기준, 조퇴 허용 10분 (17:50까지는 정상)
            // - 조퇴: 17:45 ~ 17:49
            // - 정상: 17:50 ~ 18:30
            int clockOutVariation = (memberIndex * 5 + workDayIndex * 7) % 46; // 0~45분 (46가지)
            int clockOutHour = 17;
            int clockOutMinute = 45 + clockOutVariation; // 17:45 ~ 18:30
            if (clockOutMinute >= 60) {
                clockOutHour = 18;
                clockOutMinute = clockOutMinute - 60;
            }
            clockOut = LocalDateTime.of(date, LocalTime.of(clockOutHour, clockOutMinute));

            // AttendanceLog: CLOCK_OUT
            AttendanceLog clockOutLog = AttendanceLog.builder()
                    .memberId(member.getMemberId())
                    .eventType(EventType.CLOCK_OUT)
                    .eventTime(clockOut)
                    .latitude(mainOffice.getLatitude())
                    .longitude(mainOffice.getLongitude())
                    .isCorrected(false)
                    .build();
            attendanceLogRepository.save(clockOutLog);
            logsCreated++;
        } else {
            clockOut = null;
        }

        // DailyAttendance 업서트
        final boolean isClockOutComplete = !skipClockOut;
        upsertDailyAttendance(member.getMemberId(), companyId, date, da -> {
            // 최초 생성 시에만 NORMAL_WORK로 설정 (이미 다른 상태면 유지)
            if (da.getStatus() == null) {
                da.updateStatus(AttendanceStatus.NORMAL_WORK);
            }

            da.setFirstClockIn(clockIn);
            da.setLastClockOut(clockOut);  // 퇴근 미완료면 null

            if (isClockOutComplete) {
                // 실제 출퇴근 시간에 따라 근무 시간 계산
                // 총 경과 시간 - 휴게 시간(정책 기반) = 근무 시간
                long totalMinutes = java.time.Duration.between(clockIn, clockOut).toMinutes();
                int workedMinutes = (int) (totalMinutes - breakMinutes);
                if (workedMinutes < 0) workedMinutes = 0;

                da.setWorkedMinutes(workedMinutes);
                da.setTotalBreakMinutes(breakMinutes);
            } else {
                da.setWorkedMinutes(0);
                da.setTotalBreakMinutes(0);
            }

            // 지각 판정: 09:00 기준, 10분 허용 (09:10까지 정상)
            LocalTime standardStart = LocalTime.of(9, 0);
            LocalTime latenessGraceEnd = LocalTime.of(9, 10);
            if (clockIn.toLocalTime().isAfter(latenessGraceEnd)) {
                da.setIsLate(true);
                long lateMinutes = java.time.Duration.between(
                    LocalDateTime.of(date, standardStart),
                    clockIn
                ).toMinutes();
                da.setLateMinutes((int) lateMinutes);
            } else {
                da.setIsLate(false);
                da.setLateMinutes(0);
            }

            // 조퇴 판정: 18:00 기준, 10분 허용 (17:50부터 정상)
            if (isClockOutComplete) {
                LocalTime standardEnd = LocalTime.of(18, 0);
                LocalTime earlyLeaveGraceStart = LocalTime.of(17, 50);
                if (clockOut.toLocalTime().isBefore(earlyLeaveGraceStart)) {
                    da.setIsEarlyLeave(true);
                    long earlyLeaveMinutes = java.time.Duration.between(
                        clockOut,
                        LocalDateTime.of(date, standardEnd)
                    ).toMinutes();
                    da.setEarlyLeaveMinutes((int) earlyLeaveMinutes);
                } else {
                    da.setIsEarlyLeave(false);
                    da.setEarlyLeaveMinutes(0);
                }
            } else {
                da.setIsEarlyLeave(false);
                da.setEarlyLeaveMinutes(0);
            }
        });

        return logsCreated;
    }

    /**
     * 6단계: 휴가 신청 및 결재 연동 데이터 생성 (결정적 패턴, Idempotent)
     *
     * 생성 흐름:
     * 1. Request 생성 (잔액 차감)
     * 2. Approval 생성 (requestId 연결)
     * 3. ApprovalLine 생성
     * 4. 승인/반려 처리:
     *    - APPROVED: Request 상태 업데이트, DailyAttendance 업서트
     *    - PENDING: Request 상태 유지 (대기 중)
     *
     * 결정적 패턴:
     * - 각 직원별로 승인 2개 + 대기 1개
     * - 중복 생성 방지 (Idempotent)
     */
    private void createLeaveRequests(TestEmployees employees) {
        int totalRequests = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        int pendingCount = 0;

        // 각 직원별로 5개의 휴가 신청 생성 (승인 4개 + 대기 1개)
        // 시연용: 잔액 없어도 생성 (급여 계산은 DailyAttendance + Policy.isPaid 기반)
        for (int memberIndex = 0; memberIndex < employees.all.size(); memberIndex++) {
            MemberEmploymentInfoDto member = employees.all.get(memberIndex);

            // 잔액 확인 (있으면 차감, 없으면 경고만)
            MemberBalance balance = memberBalanceRepository
                    .findByMemberIdAndBalanceTypeCodeAndYear(
                            member.getMemberId(),
                            PolicyTypeCode.ANNUAL_LEAVE,
                            DEMO_DATE.getYear())
                    .orElse(null);

            if (balance == null) {
                log.warn("      ⚠️  {} - 연차 잔액이 없습니다. (급여 계산용 DailyAttendance만 생성)", member.getName());
            } else if (balance.getRemaining() < 1.0) {
                log.warn("      ⚠️  {} - 잔액 부족({})입니다. (급여 계산용 DailyAttendance만 생성)",
                         member.getName(), balance.getRemaining());
            }

            // 결정적 패턴: 각 직원별로 5개 (승인 4 + 대기 1)
            // 최근 날짜로 변경: 더 많은 휴가 상태자 생성
            int[][] requestPatterns = {
                {-2, 1, RequestStatus.APPROVED.ordinal()},    // 11/10 (전날)
                {-3, 1, RequestStatus.APPROVED.ordinal()},    // 11/9
                {-6 - memberIndex, 1, RequestStatus.APPROVED.ordinal()}, // 11/6, 11/5, 11/4...
                {-10 - memberIndex * 2, 1, RequestStatus.APPROVED.ordinal()}, // 11/2, 10/31, 10/29...
                {7 + memberIndex, 1, RequestStatus.PENDING.ordinal()}        // 미래 (대기) - 11/19, 11/20...
            };

            for (int i = 0; i < requestPatterns.length; i++) {
                int daysOffset = requestPatterns[i][0];
                RequestStatus targetStatus = RequestStatus.values()[requestPatterns[i][2]];

                LocalDate leaveDate = DEMO_DATE.plusDays(daysOffset);

                // 주말 제외
                while (isWeekendOrHoliday(leaveDate)) {
                    leaveDate = leaveDate.plusDays(1);
                }

                // 중복 생성 방지 (Idempotent)
                LocalDateTime startDateTime = LocalDateTime.of(leaveDate, LocalTime.of(9, 0));
                LocalDateTime endDateTime = LocalDateTime.of(leaveDate, LocalTime.of(18, 0));

                // 이미 동일한 신청이 존재하는지 확인
                boolean alreadyExists = requestRepository
                        .findAll()
                        .stream()
                        .anyMatch(r ->
                                r.getMemberId().equals(member.getMemberId()) &&
                                r.getPolicy().getId().equals(annualLeavePolicy.getId()) &&
                                r.getStartDateTime().equals(startDateTime));

                if (alreadyExists) {
                    log.debug("연차 신청 이미 존재 - memberId: {}, date: {}", member.getMemberId(), leaveDate);
                    continue;
                }

                // 1. Request 생성
                Request request = Request.builder()
                        .memberId(member.getMemberId())
                        .policy(annualLeavePolicy)
                        .requestUnit(RequestUnit.DAY)
                        .status(RequestStatus.PENDING) // 초기 상태는 PENDING
                        .startDateTime(startDateTime)
                        .endDateTime(endDateTime)
                        .reason(i == 0 ? "개인 사유" : (i == 1 ? "가족 행사" : "휴식"))
                        .deductionDays(1.0)
                        .workLocation(null)
                        .completedAt(null)
                        .build();

                requestRepository.save(request);

                // 잔액 차감 (balance가 있을 때만)
                if (balance != null && balance.getRemaining() >= 1.0) {
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
                    log.debug("      - 잔액 차감: {} (남은 잔액: {})", member.getName(), balance.getRemaining());
                }

                // 2. ApprovalDocument 조회 (시스템에서 미리 등록된 템플릿 사용)
                ApprovalDocument document = approvalDocumentRepository.findByDocumentName("휴가 신청서")
                        .orElseThrow(() -> new RuntimeException("휴가 신청서 템플릿을 찾을 수 없습니다. Approval 서비스에서 먼저 등록되어야 합니다."));

                // 3. Approval 생성 (시연용: memberPositionId는 companyId 사용)
                Approval approval = Approval.builder()
                        .memberPositionId(companyId)
                        .approvalDocument(document)
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

                // 4. ApprovalLine 생성 (단일 결재자로 간소화)
                UUID approverPositionId = companyId; // 시연용: companyId 사용

                ApprovalLine line = ApprovalLine.builder()
                        .approval(approval)
                        .memberPositionId(approverPositionId)
                        .lineIndex(1)
                        .lineStatus(targetStatus == RequestStatus.APPROVED ? LineStatus.APPROVED : LineStatus.PENDING)
                        .approvalDate(targetStatus == RequestStatus.APPROVED ?
                                     DEMO_DATE.minusDays(Math.abs(daysOffset)).atTime(14, 0) : null)
                        .build();
                approvalLineRepository.save(line);

                // Approval 상태 업데이트
                if (targetStatus == RequestStatus.APPROVED) {
                    approval.updateState(ApprovalState.APPROVED);
                }

                approvalRepository.save(approval);

                // 5. Request 상태 업데이트 및 후처리
                if (targetStatus == RequestStatus.APPROVED) {
                    request.updateStatus(RequestStatus.APPROVED); // updateStatus가 completedAt도 자동 설정
                    requestRepository.save(request);

                    // DailyAttendance 업서트
                    upsertDailyAttendance(member.getMemberId(), companyId, leaveDate, da -> {
                        da.updateStatus(AttendanceStatus.ANNUAL_LEAVE);
                        da.setFirstClockIn(null);
                        da.setLastClockOut(null);
                        da.setWorkedMinutes(0);
                        da.setTotalBreakMinutes(0);
                        da.setIsLate(false);
                        da.setLateMinutes(0);
                        da.setIsEarlyLeave(false);
                        da.setEarlyLeaveMinutes(0);
                    });

                    log.info("      ✓ {} - 휴가 승인 및 DailyAttendance 생성: {} (상태: ANNUAL_LEAVE)",
                             member.getName(), leaveDate);

                    approvedCount++;
                } else {
                    // PENDING 상태 유지
                    pendingCount++;
                }

                totalRequests++;
            }
        }

        log.info("   ✓ 총 {}개의 휴가 신청 생성 (승인: {}, 반려: {}, 대기: {})",
                 totalRequests, approvedCount, rejectedCount, pendingCount);
        log.info("   ✓ 승인된 휴가에 대한 DailyAttendance {} 건 생성 (급여 계산용)", approvedCount);
        log.info("");
    }

    /**
     * 6-2단계: 출장 신청 생성 (결정적 패턴, Idempotent)
     * - 특정 직원들에게 출장 신청 데이터 생성
     * - 급여 계산 테스트용
     */
    private void createTripRequests(TestEmployees employees) {
        log.info("");
        log.info("6-2. 출장 신청 데이터 생성 시작...");

        int totalRequests = 0;
        int approvedCount = 0;

        // 출장 신청 패턴: 인덱스 1, 3, 5 직원만 (3명)
        for (int i = 0; i < employees.all.size(); i++) {
            if (i % 2 == 0) continue; // 짝수 인덱스 건너뛰기

            MemberEmploymentInfoDto member = employees.all.get(i);

            // 출장 신청: 10월 15~17일 (3일간)
            LocalDate tripStartDate = LocalDate.of(2025, 10, 15);
            LocalDate tripEndDate = LocalDate.of(2025, 10, 17);

            // 주말 건너뛰기
            while (isWeekendOrHoliday(tripStartDate)) {
                tripStartDate = tripStartDate.plusDays(1);
                tripEndDate = tripEndDate.plusDays(1);
            }

            LocalDateTime startDateTime = tripStartDate.atTime(9, 0);
            LocalDateTime endDateTime = tripEndDate.atTime(18, 0);

            // 중복 생성 방지
            boolean alreadyExists = requestRepository
                    .findAll()
                    .stream()
                    .anyMatch(r ->
                            r.getMemberId().equals(member.getMemberId()) &&
                            r.getPolicy().getId().equals(businessTripPolicy.getId()) &&
                            r.getStartDateTime().equals(startDateTime));

            if (alreadyExists) {
                log.debug("출장 신청 이미 존재 - memberId: {}", member.getMemberId());
                continue;
            }

            // Request 생성
            Request request = Request.builder()
                    .memberId(member.getMemberId())
                    .policy(businessTripPolicy)
                    .requestUnit(RequestUnit.DAY)
                    .status(RequestStatus.APPROVED) // 자동 승인
                    .startDateTime(startDateTime)
                    .endDateTime(endDateTime)
                    .reason("현장 업무 미팅")
                    .deductionDays(0.0) // 출장은 잔액 차감 없음
                    .workLocation("서울 본사")
                    .completedAt(LocalDateTime.now())
                    .build();

            requestRepository.save(request);

            // DailyAttendance 생성 (출장 기간 동안)
            LocalDate currentDate = tripStartDate;
            while (!currentDate.isAfter(tripEndDate)) {
                final LocalDate dateToProcess = currentDate;
                upsertDailyAttendance(member.getMemberId(), companyId, dateToProcess, da -> {
                    da.updateStatus(AttendanceStatus.BUSINESS_TRIP);
                    da.setFirstClockIn(dateToProcess.atTime(9, 0));
                    da.setLastClockOut(dateToProcess.atTime(18, 0));
                    da.setWorkedMinutes(480); // 8시간
                    da.setTotalBreakMinutes(60);
                });
                currentDate = currentDate.plusDays(1);
            }

            approvedCount++;
            totalRequests++;
        }

        log.info("   ✓ 총 {}개의 출장 신청 생성 (승인: {})", totalRequests, approvedCount);
        log.info("   ✓ 출장 기간 DailyAttendance {} 건 생성", approvedCount * 3);
        log.info("");
    }

    /**
     * 7단계: 추가근무 신청 생성 (결정적 패턴, Idempotent)
     * - 연장근무: 전 직원, i % 3 == 0인 평일에 120~240분 (다양한 시간)
     *   → status 유지, overtimeMinutes/daytimeOvertimeMinutes만 추가
     *   → 주간 한도: 720분(12시간) 검증
     * - 야간근무: 짝수 인덱스 직원만, 매주 수요일 밤 360~540분
     *   → status 유지, nightWorkMinutes만 추가
     * - 휴일근무: memberIndex % 3 == 0인 직원만, 매 2번째 일요일 360~540분
     *   → workedMinutes = 0, holidayWorkMinutes만 기록 (중복 방지)
     * - 출장: 첫 번째 직원만 1건
     *   → status를 BUSINESS_TRIP으로 변경
     * - DailyAttendance 업서트로 중복 방지
     */
    private void createExtraWorkRequests(TestEmployees employees) {
        int totalRequests = 0;
        int overtimeCount = 0;
        int nightWorkCount = 0;
        int holidayWorkCount = 0;
        int tripCount = 0;

        // 전월 1일 ~ 당월 DEMO_DATE 전날까지의 평일 목록
        LocalDate previousMonthStart = DEMO_DATE.minusMonths(1).withDayOfMonth(1); // 2025-10-01
        LocalDate currentMonthEnd = DEMO_DATE.minusDays(1); // 2025-11-11

        List<LocalDate> workDays = new ArrayList<>();
        for (LocalDate d = previousMonthStart; !d.isAfter(currentMonthEnd); d = d.plusDays(1)) {
            if (!isWeekendOrHoliday(d)) {
                workDays.add(d);
            }
        }

        // 전월 ~ 당월의 일요일 목록 (휴일근무용)
        List<LocalDate> sundays = new ArrayList<>();
        for (LocalDate d = previousMonthStart; !d.isAfter(currentMonthEnd); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                sundays.add(d);
            }
        }

        for (int memberIndex = 0; memberIndex < employees.all.size(); memberIndex++) {
            MemberEmploymentInfoDto member = employees.all.get(memberIndex);

            // 입사일 체크
            LocalDate joinDate = member.getJoinDate();

            // 주간 연장근무 누적 시간 (주 720분 한도)
            Map<Integer, Integer> weeklyOvertimeMap = new HashMap<>();

            // 1) 연장근무: i % 3 == 0인 평일에 120~240분 (다양한 시간)
            int overtimePattern = memberIndex % 3; // 0, 1, 2
            int overtimeMinutes = 120 + overtimePattern * 60; // 120, 180, 240분

            for (int i = 0; i < workDays.size(); i++) {
                if (i % 3 != 0) continue;

                LocalDate date = workDays.get(i);
                if (date.isBefore(joinDate)) continue;

                // 주차 계산 (ISO 8601 week)
                int weekOfYear = date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());

                // 주간 누적 연장근무 체크
                int currentWeekTotal = weeklyOvertimeMap.getOrDefault(weekOfYear, 0);
                if (currentWeekTotal + overtimeMinutes > 720) {
                    // 주간 한도 초과 시 스킵
                    continue;
                }

                // 중복 생성 방지
                int endHour = 18 + (overtimeMinutes / 60);
                LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.of(18, 0));
                LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.of(endHour, 0));

                if (isRequestExists(member.getMemberId(), overtimePolicy.getId(), startDateTime)) {
                    continue;
                }

                createExtraWorkRequest(member, overtimePolicy, PolicyTypeCode.OVERTIME,
                        "프로젝트 마감", startDateTime, endDateTime, overtimeMinutes, date);

                // 주간 누적 업데이트
                weeklyOvertimeMap.put(weekOfYear, currentWeekTotal + overtimeMinutes);

                overtimeCount++;
                totalRequests++;
            }

            // 2) 야간근무: 짝수 인덱스 직원만, 매주 수요일 밤 360~540분
            if (memberIndex % 2 == 0) {
                int nightWorkPattern = memberIndex % 4; // 0, 1, 2, 3
                int nightWorkMinutes = 360 + nightWorkPattern * 60; // 360, 420, 480, 540분

                for (LocalDate date : workDays) {
                    if (date.getDayOfWeek() != java.time.DayOfWeek.WEDNESDAY) continue;
                    if (date.isBefore(joinDate)) continue;

                    int endHour = 22 + (nightWorkMinutes / 60);
                    LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.of(22, 0));
                    LocalDateTime endDateTime;
                    if (endHour >= 24) {
                        endDateTime = LocalDateTime.of(date.plusDays(1), LocalTime.of(endHour - 24, 0));
                    } else {
                        endDateTime = LocalDateTime.of(date, LocalTime.of(endHour, 0));
                    }

                    if (isRequestExists(member.getMemberId(), nightWorkPolicy.getId(), startDateTime)) {
                        continue;
                    }

                    createExtraWorkRequest(member, nightWorkPolicy, PolicyTypeCode.NIGHT_WORK,
                            "야간 시스템 점검", startDateTime, endDateTime, nightWorkMinutes, date);
                    nightWorkCount++;
                    totalRequests++;
                }
            }

            // 3) 휴일근무: memberIndex % 3 == 0인 직원만, 매 2번째 일요일 360~540분
            if (memberIndex % 3 == 0) {
                int holidayWorkPattern = memberIndex % 4; // 0, 1, 2, 3
                int holidayWorkMinutes = 360 + holidayWorkPattern * 60; // 360, 420, 480, 540분

                for (int i = 1; i < sundays.size(); i += 2) { // 1, 3, 5... (2번째, 4번째...)
                    LocalDate date = sundays.get(i);
                    if (date.isBefore(joinDate)) continue;

                    int endHour = 9 + (holidayWorkMinutes / 60);
                    LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.of(9, 0));
                    LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.of(endHour, 0));

                    if (isRequestExists(member.getMemberId(), holidayWorkPolicy.getId(), startDateTime)) {
                        continue;
                    }

                    createExtraWorkRequest(member, holidayWorkPolicy, PolicyTypeCode.HOLIDAY_WORK,
                            "긴급 업무 처리", startDateTime, endDateTime, holidayWorkMinutes, date);
                    holidayWorkCount++;
                    totalRequests++;
                }
            }

            // 4) 출장: 첫 번째 직원만 1건
            if (memberIndex == 0 && !workDays.isEmpty()) {
                LocalDate date = workDays.get(workDays.size() - 5); // 5일 전
                if (!date.isBefore(joinDate)) {
                    LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.of(9, 0));
                    LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.of(18, 0));

                    if (!isRequestExists(member.getMemberId(), businessTripPolicy.getId(), startDateTime)) {
                        createExtraWorkRequest(member, businessTripPolicy, PolicyTypeCode.BUSINESS_TRIP,
                                "거래처 방문", startDateTime, endDateTime, 480, date);
                        tripCount++;
                        totalRequests++;
                    }
                }
            }
        }

        log.info("   ✓ 추가근무 신청 생성 완료:");
        log.info("      - 총 {}건 (연장: {}, 야간: {}, 휴일: {}, 출장: {})",
                totalRequests, overtimeCount, nightWorkCount, holidayWorkCount, tripCount);
        log.info("");
    }

    private boolean isWeekendOrHoliday(LocalDate date) {
        return date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * Request 중복 체크 (Idempotent)
     */
    private boolean isRequestExists(UUID memberId, UUID policyId, LocalDateTime startDateTime) {
        return requestRepository
                .findAll()
                .stream()
                .anyMatch(r ->
                        r.getMemberId().equals(memberId) &&
                        r.getPolicy().getId().equals(policyId) &&
                        r.getStartDateTime().equals(startDateTime));
    }

    /**
     * 추가근무 신청 생성 (Request + Approval + ApprovalLine + DailyAttendance 업서트)
     */
    private void createExtraWorkRequest(
            MemberEmploymentInfoDto member,
            Policy policy,
            PolicyTypeCode policyType,
            String reason,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int workMinutes,
            LocalDate requestDate) {

        // 1. Request 생성
        Request request = Request.builder()
                .memberId(member.getMemberId())
                .policy(policy)
                .requestUnit(RequestUnit.TIME_OFF)
                .status(RequestStatus.PENDING)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .reason(reason)
                .deductionDays(0.0) // 추가근무는 차감 없음
                .workLocation(policyType == PolicyTypeCode.BUSINESS_TRIP ? mainOffice.getName() : null)
                .completedAt(null)
                .build();

        requestRepository.save(request);

        // 2. ApprovalDocument 조회 (시스템에서 미리 등록된 템플릿 사용)
        // PolicyTypeCode에 따라 올바른 문서 템플릿 선택
        String documentName;
        if (policyType == PolicyTypeCode.BUSINESS_TRIP) {
            documentName = "출장 신청서";
        } else {
            // 연장/야간/휴일 모두 "추가근무 신청서" 템플릿 공통 사용
            documentName = "추가근무 신청서";
        }
        ApprovalDocument document = approvalDocumentRepository.findByDocumentName(documentName)
                .orElseThrow(() -> new RuntimeException(documentName + " 템플릿을 찾을 수 없습니다. ApprovalDocumentInitializer에서 먼저 등록되어야 합니다."));

        // 3. Approval 생성
        Approval approval = Approval.builder()
                .memberPositionId(companyId)
                .approvalDocument(document)
                .title(member.getName() + "님의 " + policy.getName() + " 신청")
                .contents(Map.of(
                        "startDateTime", startDateTime.toString(),
                        "endDateTime", endDateTime.toString(),
                        "reason", reason,
                        "type", policy.getName()
                ))
                .state(ApprovalState.PENDING)
                .build();

        approvalRepository.save(approval);

        // Request에 approvalId 연결
        request.updateApprovalId(approval.getId());
        requestRepository.save(request);

        // 4. ApprovalLine 생성 (자동 승인)
        ApprovalLine line = ApprovalLine.builder()
                .approval(approval)
                .memberPositionId(companyId)
                .lineIndex(1)
                .lineStatus(LineStatus.APPROVED)
                .approvalDate(startDateTime.minusDays(1))
                .build();
        approvalLineRepository.save(line);

        // Approval 상태 업데이트
        approval.updateState(ApprovalState.APPROVED);
        approvalRepository.save(approval);

        // 5. Request 상태 업데이트 및 DailyAttendance 업서트
        request.updateStatus(RequestStatus.APPROVED);
        requestRepository.save(request);

        // DailyAttendance 업서트
        upsertDailyAttendance(member.getMemberId(), companyId, requestDate, da -> {
            // 출장: 하루 종일이므로 상태 변경
            if (policyType == PolicyTypeCode.BUSINESS_TRIP) {
                da.updateStatus(AttendanceStatus.BUSINESS_TRIP);
                da.setFirstClockIn(startDateTime);
                da.setLastClockOut(endDateTime);
                da.setWorkedMinutes(workMinutes);
                da.setTotalBreakMinutes(60);
            }
            // 휴일근무: 휴일에 근무 (기본 출퇴근 없는 날)
            else if (policyType == PolicyTypeCode.HOLIDAY_WORK) {
                da.addHolidayWorkMinutes(workMinutes);
                // 휴일이므로 기본 출퇴근이 없음 → firstClockIn/lastClockOut 설정
                if (da.getFirstClockIn() == null) {
                    da.setFirstClockIn(startDateTime);
                    da.setLastClockOut(endDateTime);
                    da.setWorkedMinutes(0); // 휴일근무는 기본 근무 시간이 없고 추가 근무만 있음
                    da.setTotalBreakMinutes(60);
                }
            }
            // 연장근무: 평일 기본 근무 + 추가 시간 (status 유지, minutes만 추가)
            else if (policyType == PolicyTypeCode.OVERTIME) {
                da.addOvertimeMinutes(workMinutes);
                da.addDaytimeOvertimeMinutes(workMinutes);
            }
            // 야간근무: 평일 기본 근무 + 야간 시간 (status 유지, minutes만 추가)
            else if (policyType == PolicyTypeCode.NIGHT_WORK) {
                da.addNightWorkMinutes(workMinutes);
            }
        });
    }

    /**
     * DailyAttendance 업서트 유틸리티
     * - 동일 (memberId, attendanceDate) 조합이 존재하면 mutator로 수정
     * - 없으면 새로 생성 후 mutator 적용
     * - 모든 DailyAttendance 생성/수정은 이 메서드를 통해서만 수행
     */
    private DailyAttendance upsertDailyAttendance(UUID memberId, UUID companyId, LocalDate date, java.util.function.Consumer<DailyAttendance> mutator) {
        DailyAttendance da = dailyAttendanceRepository.findByMemberIdAndAttendanceDate(memberId, date).orElse(null);
        if (da == null) {
            da = DailyAttendance.builder()
                    .memberId(memberId)
                    .companyId(companyId)
                    .attendanceDate(date)
                    .status(AttendanceStatus.NORMAL_WORK)
                    .workedMinutes(0)
                    .totalBreakMinutes(0)
                    .overtimeMinutes(0)
                    .daytimeOvertimeMinutes(0)
                    .nightWorkMinutes(0)
                    .holidayWorkMinutes(0)
                    .isLate(false)
                    .lateMinutes(0)
                    .isEarlyLeave(false)
                    .earlyLeaveMinutes(0)
                    .build();
        }
        mutator.accept(da);
        return dailyAttendanceRepository.save(da);
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
