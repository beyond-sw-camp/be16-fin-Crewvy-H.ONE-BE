package com.crewvy.workforce_service.attendance.config;

import com.crewvy.workforce_service.attendance.constant.*;
import com.crewvy.workforce_service.attendance.dto.rule.*;
import com.crewvy.workforce_service.attendance.entity.Policy;
import com.crewvy.workforce_service.attendance.entity.PolicyAssignment;
import com.crewvy.workforce_service.attendance.entity.PolicyType;
import com.crewvy.workforce_service.attendance.entity.Request;
import com.crewvy.workforce_service.attendance.repository.PolicyAssignmentRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyTypeRepository;
import com.crewvy.workforce_service.attendance.repository.RequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * 로컬 환경 테스트용 근태 더미 데이터 자동 생성
 *
 * 전제조건: member-service가 먼저 실행되어 emp1@h.one ~ emp10@h.one 계정이 생성되어 있어야 함
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class AttendanceTestDataInitializer implements ApplicationRunner {

    @PersistenceContext
    private final EntityManager entityManager;

    private final PolicyTypeRepository policyTypeRepository;
    private final PolicyRepository policyRepository;
    private final PolicyAssignmentRepository policyAssignmentRepository;
    private final RequestRepository requestRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== 🚀 Attendance Test Data Initialization Started ===");

        try {
            // 1. member 테이블에서 테스트 계정 조회
            List<Map<String, Object>> testMembers = findTestMembers();

            if (testMembers.isEmpty()) {
                log.warn("⚠️  No test members found (emp1@h.one ~ emp10@h.one).");
                log.warn("⚠️  Please start member-service first to create test accounts.");
                return;
            }

            log.info("✅ Found {} test members", testMembers.size());

            // 각 회사별로 처리
            Map<UUID, List<Map<String, Object>>> membersByCompany = groupByCompany(testMembers);

            for (Map.Entry<UUID, List<Map<String, Object>>> entry : membersByCompany.entrySet()) {
                UUID companyId = entry.getKey();
                List<Map<String, Object>> members = entry.getValue();

                log.info("📋 Processing company: {}", companyId);

                // 2. 정책 타입 생성
                PolicyType workPolicyType = createWorkPolicyType(companyId);

                // 3. 근무 정책 생성
                Policy workPolicy = createWorkPolicy(companyId, workPolicyType);

                // 4. 회사 전체에 정책 할당
                assignPolicyToCompany(companyId, workPolicy);

                // 5. 각 직원에게 디바이스 등록
                for (Map<String, Object> member : members) {
                    UUID memberId = (UUID) member.get("member_id");
                    String email = (String) member.get("email");
                    createTestDevices(memberId, email, workPolicy);
                }
            }

            log.info("=== ✅ Attendance Test Data Initialization Completed ===");

        } catch (Exception e) {
            log.error("❌ Failed to initialize test data", e);
        }
    }

    /**
     * member 테이블에서 테스트 계정 조회
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> findTestMembers() {
        String sql = """
            SELECT m.id, m.email, m.company_id
            FROM member m
            WHERE m.email LIKE 'emp%@h.one'
            ORDER BY m.email
            """;

        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

        List<Map<String, Object>> members = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> member = new HashMap<>();
            member.put("member_id", UUID.fromString(row[0].toString()));
            member.put("email", row[1]);
            member.put("company_id", UUID.fromString(row[2].toString()));
            members.add(member);
        }

        return members;
    }

    /**
     * 회사별로 그룹핑
     */
    private Map<UUID, List<Map<String, Object>>> groupByCompany(List<Map<String, Object>> members) {
        Map<UUID, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> member : members) {
            UUID companyId = (UUID) member.get("company_id");
            grouped.computeIfAbsent(companyId, k -> new ArrayList<>()).add(member);
        }
        return grouped;
    }

    /**
     * 근무 정책 타입 생성
     */
    private PolicyType createWorkPolicyType(UUID companyId) {
        Optional<PolicyType> existing = policyTypeRepository.findByCompanyIdAndTypeCode(companyId, PolicyTypeCode.STANDARD_WORK);

        if (existing.isPresent()) {
            log.info("⏭️  Work policy type already exists");
            return existing.get();
        }

        PolicyType policyType = PolicyType.builder()
                .companyId(companyId)
                .typeCode(PolicyTypeCode.STANDARD_WORK)
                .typeName("기본근무")
                .balanceDeductible(false)
                .categoryCode(PolicyCategory.WORK_SCHEDULE)
                .build();

        policyTypeRepository.save(policyType);
        log.info("✅ Created work policy type");
        return policyType;
    }

    /**
     * 근무 정책 생성
     */
    private Policy createWorkPolicy(UUID companyId, PolicyType policyType) {
        // 이미 존재하는지 확인
        Optional<Policy> existing = policyRepository.findAll().stream()
                .filter(p -> p.getCompanyId().equals(companyId))
                .filter(p -> p.getPolicyType().equals(policyType))
                .filter(Policy::getIsActive)
                .findFirst();

        if (existing.isPresent()) {
            log.info("⏭️  Work policy already exists");
            return existing.get();
        }

        // PolicyRuleDetails 구성
        PolicyRuleDetails ruleDetails = new PolicyRuleDetails();

        // 1. 근무시간 규칙
        WorkTimeRuleDto workTimeRule = new WorkTimeRuleDto();
        workTimeRule.setType("FIXED");
        workTimeRule.setFixedWorkMinutes(480);  // 8시간
        workTimeRule.setWorkStartTime("09:00");
        workTimeRule.setWorkEndTime("18:00");
        workTimeRule.setMandatoryBreakMinutes(60);
        ruleDetails.setWorkTimeRule(workTimeRule);

        // 2. 지각/조퇴 규칙
        LatenessRuleDto latenessRule = new LatenessRuleDto();
        latenessRule.setLatenessGraceMinutes(10);
        latenessRule.setEarlyLeaveGraceMinutes(10);
        ruleDetails.setLatenessRule(latenessRule);

        // 3. 인증 규칙
        AuthRuleDto authRule = new AuthRuleDto();
        List<AuthMethodDto> methods = new ArrayList<>();

        // MOBILE - GPS 인증
        AuthMethodDto mobileAuth = new AuthMethodDto();
        mobileAuth.setDeviceType(DeviceType.MOBILE);
        mobileAuth.setAuthMethod("GPS");
        Map<String, Object> gpsDetails = new HashMap<>();
        gpsDetails.put("gpsRadiusMeters", 500);
        gpsDetails.put("officeLatitude", 37.5012743);
        gpsDetails.put("officeLongitude", 127.0396597);
        mobileAuth.setDetails(gpsDetails);
        methods.add(mobileAuth);

        // LAPTOP - IP 인증
        AuthMethodDto laptopAuth = new AuthMethodDto();
        laptopAuth.setDeviceType(DeviceType.LAPTOP);
        laptopAuth.setAuthMethod("NETWORK_IP");
        Map<String, Object> ipDetails = new HashMap<>();
        ipDetails.put("allowedIps", Arrays.asList("127.0.0.1", "::1", "192.168.0.1", "192.168.0.100", "172.20.224.1")); // cmd-ipconfig: ipv4주소 등록함
        laptopAuth.setDetails(ipDetails);
        methods.add(laptopAuth);

        authRule.setMethods(methods);
        ruleDetails.setAuthRule(authRule);

        // 4. 외출 규칙
        GoOutRuleDto goOutRule = new GoOutRuleDto();
        goOutRule.setType("SIMPLE_RECORD");  // 단순 기록
        goOutRule.setMaxDailyGoOutMinutes(120);  // 일일 최대 2시간
        goOutRule.setMaxSingleGoOutMinutes(60);  // 1회 최대 1시간
        ruleDetails.setGoOutRule(goOutRule);

        // 5. 휴게 규칙
        BreakRuleDto breakRule = new BreakRuleDto();
        breakRule.setType("MANUAL");  // 직접 기록
        breakRule.setMandatoryBreakMinutes(60);  // 법정 최소 60분
        breakRule.setMaxDailyBreakMinutes(90);  // 일일 최대 90분
        ruleDetails.setBreakRule(breakRule);

        // 6. 퇴근 규칙
        ClockOutRuleDto clockOutRule = new ClockOutRuleDto();
        clockOutRule.setAllowDuplicateClockOut(true);  // 퇴근 중복 허용
        clockOutRule.setLimitType("FIXED_PLUS_HOURS");  // 정규 퇴근 + N시간
        clockOutRule.setMaxHoursAfterWorkEnd(3);  // 정규 퇴근(18:00) + 3시간 = 21:00까지 허용
        ruleDetails.setClockOutRule(clockOutRule);

        // Policy 생성
        Policy policy = Policy.builder()
                .policyType(policyType)
                .companyId(companyId)
                .name("테스트 출퇴근 정책")
                .ruleDetails(ruleDetails)
                .isPaid(true)
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .effectiveTo(LocalDate.of(2025, 12, 31))
                .isActive(true)
                .build();

        policyRepository.save(policy);
        log.info("✅ Created work policy with auth rules (MOBILE: GPS, LAPTOP: IP)");
        return policy;
    }

    /**
     * 회사 전체에 정책 할당
     */
    private void assignPolicyToCompany(UUID companyId, Policy policy) {
        boolean exists = policyAssignmentRepository.findAll().stream()
                .anyMatch(pa -> pa.getPolicy().equals(policy)
                        && pa.getTargetId().equals(companyId)
                        && pa.getTargetType().name().equals("COMPANY"));

        if (exists) {
            log.info("⏭️  Policy already assigned to company");
            return;
        }

        PolicyAssignment assignment = PolicyAssignment.builder()
                .policy(policy)
                .targetId(companyId)
                .targetType(com.crewvy.workforce_service.attendance.constant.PolicyScopeType.COMPANY)
                .isActive(true)
                .assignedBy(companyId)  // 임시로 companyId 사용
                .assignedAt(java.time.LocalDateTime.now())  // 할당 시각
                .build();

        policyAssignmentRepository.save(assignment);
        log.info("✅ Assigned policy to company");
    }

    /**
     * 테스트 디바이스 등록 (MOBILE, LAPTOP)
     */
    private void createTestDevices(UUID memberId, String email, Policy policy) {
        // 이미 존재하는지 확인
        boolean exists = requestRepository.existsApprovedDevice(
                memberId,
                "test-mobile-" + email,
                DeviceType.MOBILE,
                RequestStatus.APPROVED
        );

        if (exists) {
            log.info("⏭️  Devices already exist for {}", email);
            return;
        }

        // MOBILE 디바이스
        Request mobileDevice = Request.builder()
                .policy(policy)
                .memberId(memberId)
                .documentId(UUID.randomUUID())
                .requestUnit(RequestUnit.DAY)
                .startAt(LocalDate.now())
                .endAt(LocalDate.now())
                .deductionDays(0.0)
                .reason("Test mobile device")
                .status(RequestStatus.APPROVED)
                .deviceId("test-mobile-" + email)
                .deviceName("Test iPhone (" + email + ")")
                .deviceType(DeviceType.MOBILE)
                .build();

        // LAPTOP 디바이스
        Request laptopDevice = Request.builder()
                .policy(policy)
                .memberId(memberId)
                .documentId(UUID.randomUUID())
                .requestUnit(RequestUnit.DAY)
                .startAt(LocalDate.now())
                .endAt(LocalDate.now())
                .deductionDays(0.0)
                .reason("Test laptop device")
                .status(RequestStatus.APPROVED)
                .deviceId("test-laptop-" + email)
                .deviceName("Test MacBook (" + email + ")")
                .deviceType(DeviceType.LAPTOP)
                .build();

        requestRepository.saveAll(Arrays.asList(mobileDevice, laptopDevice));
        log.info("✅ Created 2 devices for {}", email);
    }
}
