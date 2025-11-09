package com.crewvy.workforce_service.attendance.service;

import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import com.crewvy.workforce_service.attendance.repository.DailyAttendanceRepository;
import com.crewvy.workforce_service.attendance.repository.MemberBalanceRepository;
import com.crewvy.workforce_service.attendance.repository.PolicyRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberEmploymentInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 연차 자동 발생 서비스
 * 근로기준법 제60조 기반 연차 계산 및 발생
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnualLeaveAccrualService {

    private final MemberBalanceRepository memberBalanceRepository;
    private final MemberClient memberClient;
    private final PolicyRepository policyRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;

    // 배치 처리 청크 크기 (성능 최적화: 100명씩 나눠서 처리)
    private static final int BATCH_CHUNK_SIZE = 100;

    /**
     * 회사의 모든 활성 직원에 대해 연차 자동 발생
     * 청크 기반 처리로 대량 데이터 성능 최적화
     * @param companyId 회사 ID
     * @param referenceDate 기준 날짜 (보통 현재 날짜)
     */
    @Transactional
    public void accrueAnnualLeaveForCompany(UUID companyId, LocalDate referenceDate) {
        log.info("========================================");
        log.info("연차 자동 발생 시작: companyId={}, referenceDate={}", companyId, referenceDate);
        log.info("========================================");

        try {
            // 1. 회사의 연차 정책 조회
            Optional<com.crewvy.workforce_service.attendance.entity.Policy> annualLeavePolicyOpt =
                    policyRepository.findActiveAnnualLeavePolicy(companyId);

            if (annualLeavePolicyOpt.isEmpty()) {
                log.warn("회사의 연차 정책이 없어 배치 중단: companyId={}", companyId);
                return;
            }

            com.crewvy.workforce_service.attendance.entity.Policy annualLeavePolicy = annualLeavePolicyOpt.get();
            log.info("연차 정책 확인: policyName={}", annualLeavePolicy.getName());

            // standardType 확인
            var leaveRule = annualLeavePolicy.getRuleDetails().getLeaveRule();
            String standardType = leaveRule.getStandardType();

            if ("JOIN_DATE".equals(standardType)) {
                // 입사일 기준: 매일 배치에서 오늘이 입사일 기준 1년이 되는 직원만 처리
                accrueByJoinDateStandard(companyId, referenceDate, annualLeavePolicy);
                return;
            }

            // FISCAL_YEAR 기준 (기본값): 매년 1월 1일 일괄 부여
            log.info("연차 기준 유형: FISCAL_YEAR (회계연도)");

            // 2. 회사의 모든 직원 조회 (내부 전용 API 사용)
            List<MemberEmploymentInfoDto> members = memberClient
                    .getEmploymentInfoInternal(companyId)
                    .getData();

            if (members == null || members.isEmpty()) {
                log.info("연차 발생 대상 직원 없음: companyId={}", companyId);
                return;
            }

            // 3. 1년 이상 근속자 필터링
            List<MemberEmploymentInfoDto> regularMembers = members.stream()
                .filter(m -> "MS001".equals(m.getMemberStatus()) && m.getJoinDate() != null) // MS001 = WORKING (재직)
                .filter(m -> Period.between(m.getJoinDate(), referenceDate).getYears() >= 1)
                .toList();

            if (regularMembers.isEmpty()) {
                log.info("1년 이상 근속 직원 없음: companyId={}", companyId);
                return;
            }

            log.info(">>> 처리 대상 직원 수: {}", regularMembers.size());

            // 3. 청크 단위로 나눠서 처리
            int totalMembers = regularMembers.size();
            int currentYear = referenceDate.getYear();
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (int i = 0; i < totalMembers; i += BATCH_CHUNK_SIZE) {
                int chunkStart = i;
                int chunkEnd = Math.min(i + BATCH_CHUNK_SIZE, totalMembers);
                List<MemberEmploymentInfoDto> chunk = regularMembers.subList(chunkStart, chunkEnd);

                log.info(">>> 청크 처리 중: {}-{}/{}", chunkStart + 1, chunkEnd, totalMembers);

                try {
                    // Level 1: 청크 단위 일괄 처리 (빠름)
                    int[] chunkResult = processChunkForRegularMembers(chunk, referenceDate, currentYear, annualLeavePolicy);
                    successCount += chunkResult[0];
                    skipCount += chunkResult[1];

                } catch (Exception e) {
                    log.error(">>> 청크 일괄 처리 실패: {}-{}, 개별 재시도 시작", chunkStart + 1, chunkEnd, e);

                    // Level 2: 개별 재시도 (안전)
                    int[] retryResult = retryIndividuallyForRegularMembers(chunk, referenceDate, currentYear, annualLeavePolicy);
                    successCount += retryResult[0];
                    skipCount += retryResult[1];
                    failCount += retryResult[2];
                }
            }

            log.info("========================================");
            log.info("연차 자동 발생 완료: 성공={}, 스킵={}, 실패={}", successCount, skipCount, failCount);
            log.info("========================================");

        } catch (Exception e) {
            log.error("연차 자동 발생 중 치명적 오류 발생: companyId={}", companyId, e);
            throw e;
        }
    }

    /**
     * 청크 단위 일괄 처리 (1년 이상 근속자)
     * @return [성공 수, 스킵 수]
     */
    private int[] processChunkForRegularMembers(List<MemberEmploymentInfoDto> chunk, LocalDate referenceDate, int currentYear,
                                               com.crewvy.workforce_service.attendance.entity.Policy policy) {
        // 청크 내 회원 ID 추출
        List<UUID> chunkMemberIds = chunk.stream()
                .map(MemberEmploymentInfoDto::getMemberId)
                .toList();

        // 기존 연차 잔액 조회 (청크 단위)
        List<MemberBalance> existingBalances = memberBalanceRepository
                .findByMemberIdInAndBalanceTypeCodeAndYear(
                        chunkMemberIds,
                        PolicyTypeCode.ANNUAL_LEAVE,
                        currentYear
                );

        Map<UUID, MemberBalance> balanceMap = existingBalances.stream()
                .collect(Collectors.toMap(MemberBalance::getMemberId, Function.identity()));

        // 신규 발생 대상 필터링
        List<MemberBalance> balancesToSave = new ArrayList<>();
        int successCount = 0;
        int skipCount = 0;

        for (MemberEmploymentInfoDto member : chunk) {
            if (!balanceMap.containsKey(member.getMemberId())) {
                accrueAnnualLeaveForRegularMember(member, referenceDate, balancesToSave, policy);
                successCount++;
            } else {
                skipCount++;
            }
        }

        // 일괄 저장 (bulk insert)
        if (!balancesToSave.isEmpty()) {
            memberBalanceRepository.saveAll(balancesToSave);
        }

        return new int[]{successCount, skipCount};
    }

    /**
     * 개별 재시도 처리 (1년 이상 근속자)
     * 청크 실패 시 한 명씩 재시도하여 피해 최소화
     * @return [성공 수, 스킵 수, 실패 수]
     */
    private int[] retryIndividuallyForRegularMembers(List<MemberEmploymentInfoDto> chunk, LocalDate referenceDate, int currentYear,
                                                     com.crewvy.workforce_service.attendance.entity.Policy policy) {
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (MemberEmploymentInfoDto member : chunk) {
            try {
                // 개별 연차 잔액 조회
                Optional<MemberBalance> existingBalance = memberBalanceRepository
                        .findByMemberIdAndBalanceTypeCodeAndYear(
                                member.getMemberId(),
                                PolicyTypeCode.ANNUAL_LEAVE,
                                currentYear
                        );

                if (existingBalance.isEmpty()) {
                    List<MemberBalance> balancesToSave = new ArrayList<>();
                    accrueAnnualLeaveForRegularMember(member, referenceDate, balancesToSave, policy);
                    memberBalanceRepository.saveAll(balancesToSave);
                    successCount++;
                } else {
                    skipCount++;
                }

            } catch (Exception e) {
                failCount++;
                log.error(">>> 개별 처리 실패: memberId={}, memberName={}",
                        member.getMemberId(), member.getName(), e);
            }
        }

        return new int[]{successCount, skipCount, failCount};
    }

    /**
     * JOIN_DATE 기준 연차 발생
     * 매일 배치에서 오늘이 입사일 기준 N년이 되는 직원에게만 연차 부여
     */
    private void accrueByJoinDateStandard(UUID companyId, LocalDate referenceDate,
                                         com.crewvy.workforce_service.attendance.entity.Policy policy) {
        log.info("연차 기준 유형: JOIN_DATE (입사일 기준)");

        try {
            // 1. 회사의 모든 직원 조회
            List<MemberEmploymentInfoDto> members = memberClient
                    .getEmploymentInfoInternal(companyId)
                    .getData();

            if (members == null || members.isEmpty()) {
                log.info("연차 발생 대상 직원 없음: companyId={}", companyId);
                return;
            }

            // 2. 오늘이 입사 기념일인 직원 필터링 (1년 이상 근속)
            List<MemberEmploymentInfoDto> anniversaryMembers = members.stream()
                    .filter(m -> "MS001".equals(m.getMemberStatus()) && m.getJoinDate() != null) // MS001 = WORKING (재직)
                    .filter(m -> {
                        LocalDate joinDate = m.getJoinDate();
                        int yearsOfService = Period.between(joinDate, referenceDate).getYears();

                        // 1년 이상 근속이면서 오늘이 입사 기념일 (월/일이 같음)
                        return yearsOfService >= 1
                                && joinDate.getMonthValue() == referenceDate.getMonthValue()
                                && joinDate.getDayOfMonth() == referenceDate.getDayOfMonth();
                    })
                    .toList();

            if (anniversaryMembers.isEmpty()) {
                log.info("입사 기념일 해당 직원 없음: referenceDate={}", referenceDate);
                return;
            }

            log.info(">>> 입사 기념일 처리 대상: {}명", anniversaryMembers.size());

            // 3. 청크 단위로 처리
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (int i = 0; i < anniversaryMembers.size(); i += BATCH_CHUNK_SIZE) {
                int chunkEnd = Math.min(i + BATCH_CHUNK_SIZE, anniversaryMembers.size());
                List<MemberEmploymentInfoDto> chunk = anniversaryMembers.subList(i, chunkEnd);

                try {
                    int[] result = processChunkForJoinDateStandard(chunk, referenceDate, policy);
                    successCount += result[0];
                    skipCount += result[1];
                } catch (Exception e) {
                    log.error("청크 처리 실패, 개별 재시도: {}-{}", i + 1, chunkEnd, e);
                    int[] retryResult = retryIndividuallyForJoinDateStandard(chunk, referenceDate, policy);
                    successCount += retryResult[0];
                    skipCount += retryResult[1];
                    failCount += retryResult[2];
                }
            }

            log.info("입사일 기준 연차 발생 완료: 성공={}, 스킵={}, 실패={}", successCount, skipCount, failCount);

        } catch (Exception e) {
            log.error("입사일 기준 연차 발생 중 오류: companyId={}", companyId, e);
            throw e;
        }
    }

    /**
     * JOIN_DATE 기준 청크 처리
     */
    private int[] processChunkForJoinDateStandard(List<MemberEmploymentInfoDto> chunk, LocalDate referenceDate,
                                                  com.crewvy.workforce_service.attendance.entity.Policy policy) {
        List<UUID> memberIds = chunk.stream().map(MemberEmploymentInfoDto::getMemberId).toList();

        // 올해 이미 부여받은 직원 조회
        List<MemberBalance> existingBalances = memberBalanceRepository
                .findByMemberIdInAndBalanceTypeCodeAndYear(
                        memberIds,
                        PolicyTypeCode.ANNUAL_LEAVE,
                        referenceDate.getYear()
                );

        Set<UUID> existingMemberIds = existingBalances.stream()
                .map(MemberBalance::getMemberId)
                .collect(Collectors.toSet());

        List<MemberBalance> balancesToSave = new ArrayList<>();
        int successCount = 0;
        int skipCount = 0;

        for (MemberEmploymentInfoDto member : chunk) {
            if (!existingMemberIds.contains(member.getMemberId())) {
                accrueAnnualLeaveForJoinDateMember(member, referenceDate, balancesToSave, policy);
                successCount++;
            } else {
                skipCount++;
            }
        }

        if (!balancesToSave.isEmpty()) {
            memberBalanceRepository.saveAll(balancesToSave);
        }

        return new int[]{successCount, skipCount};
    }

    /**
     * JOIN_DATE 기준 개별 재시도
     */
    private int[] retryIndividuallyForJoinDateStandard(List<MemberEmploymentInfoDto> chunk, LocalDate referenceDate,
                                                       com.crewvy.workforce_service.attendance.entity.Policy policy) {
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (MemberEmploymentInfoDto member : chunk) {
            try {
                Optional<MemberBalance> existingBalance = memberBalanceRepository
                        .findByMemberIdAndBalanceTypeCodeAndYear(
                                member.getMemberId(),
                                PolicyTypeCode.ANNUAL_LEAVE,
                                referenceDate.getYear()
                        );

                if (existingBalance.isEmpty()) {
                    List<MemberBalance> balancesToSave = new ArrayList<>();
                    accrueAnnualLeaveForJoinDateMember(member, referenceDate, balancesToSave, policy);
                    memberBalanceRepository.saveAll(balancesToSave);
                    successCount++;
                } else {
                    skipCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.error("개별 처리 실패: memberId={}", member.getMemberId(), e);
            }
        }

        return new int[]{successCount, skipCount, failCount};
    }

    /**
     * JOIN_DATE 기준 단일 직원 연차 부여
     */
    private void accrueAnnualLeaveForJoinDateMember(MemberEmploymentInfoDto member, LocalDate referenceDate,
                                                    List<MemberBalance> balancesToSave,
                                                    com.crewvy.workforce_service.attendance.entity.Policy policy) {
        int yearsOfService = Period.between(member.getJoinDate(), referenceDate).getYears();
        double annualLeaveToGrant = calculateRegularAnnualLeave(yearsOfService, policy);

        // 입사일 기준이므로 year는 입사일 기준으로 설정
        int balanceYear = referenceDate.getYear();

        MemberBalance newBalance = MemberBalance.builder()
                .memberId(member.getMemberId())
                .companyId(member.getCompanyId())
                .balanceTypeCode(PolicyTypeCode.ANNUAL_LEAVE)
                .year(balanceYear)
                .totalGranted(annualLeaveToGrant)
                .totalUsed(0.0)
                .remaining(annualLeaveToGrant)
                .expirationDate(referenceDate.plusYears(1).minusDays(1)) // 1년 후 만료
                .isPaid(true)
                .build();

        balancesToSave.add(newBalance);
        log.info("입사일 기준 연차 발생: memberId={}, 입사일={}, 근속={}년, 발생일수={}일",
                member.getMemberId(), member.getJoinDate(), yearsOfService, annualLeaveToGrant);
    }

    private void accrueAnnualLeaveForRegularMember(MemberEmploymentInfoDto member, LocalDate referenceDate, List<MemberBalance> balancesToSave,
                                                   com.crewvy.workforce_service.attendance.entity.Policy policy) {
        int yearsOfService = Period.between(member.getJoinDate(), referenceDate).getYears();
        double annualLeaveToGrant = calculateRegularAnnualLeave(yearsOfService, policy);
        int currentYear = referenceDate.getYear();

        MemberBalance newBalance = MemberBalance.builder()
                .memberId(member.getMemberId())
                .companyId(member.getCompanyId())
                .balanceTypeCode(PolicyTypeCode.ANNUAL_LEAVE)
                .year(currentYear)
                .totalGranted(annualLeaveToGrant)
                .totalUsed(0.0)
                .remaining(annualLeaveToGrant)
                .expirationDate(LocalDate.of(currentYear, 12, 31))
                .isPaid(true)
                .build();

        balancesToSave.add(newBalance);
        log.info("연차 발생 완료: memberId={}, year={}, 발생일수={}",
                member.getMemberId(), currentYear, annualLeaveToGrant);
    }

    /**
     * 신규 직원 초기 연차 부여 (이벤트 기반)
     * Member-Service의 member-create 이벤트 수신 시 호출
     *
     * @param memberId 신규 직원 ID
     */
    @Transactional
    public void grantInitialAnnualLeave(UUID memberId) {
        log.info("신규 직원 초기 연차 부여 시작: memberId={}", memberId);

        try {
            // 1. 직원 정보 조회 (내부 전용 API 사용)
            MemberEmploymentInfoDto memberInfo = memberClient
                    .getMemberEmploymentInfoInternal(memberId)
                    .getData();

            if (memberInfo == null) {
                log.warn("직원 정보를 찾을 수 없음: memberId={}", memberId);
                return;
            }

            // 2. 입사일 검증
            if (memberInfo.getJoinDate() == null) {
                log.warn("입사일이 없어 연차 부여 불가: memberId={}", memberId);
                return;
            }

            // 3. 이미 연차가 있는지 확인 (멱등성 보장)
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            Optional<MemberBalance> existingBalance = memberBalanceRepository
                    .findByMemberIdAndBalanceTypeCodeAndYear(
                            memberId,
                            PolicyTypeCode.ANNUAL_LEAVE,
                            currentYear
                    );

            if (existingBalance.isPresent()) {
                log.info("이미 연차가 부여되어 있음: memberId={}, 기존 연차={}일",
                        memberId, existingBalance.get().getTotalGranted());
                return;
            }

            // 4. 회사 연차 정책 조회 (성능 최적화: 우선순위 조회 불필요)
            // 연차 정책은 회사 레벨에만 할당되므로 직접 조회 가능
            Optional<com.crewvy.workforce_service.attendance.entity.Policy> annualLeavePolicyOpt =
                    policyRepository.findByCompanyIdAndPolicyTypeCode(
                            memberInfo.getCompanyId(),
                            PolicyTypeCode.ANNUAL_LEAVE
                    );

            if (annualLeavePolicyOpt.isEmpty()) {
                log.warn("회사의 연차 정책이 없어 연차 부여 불가: companyId={}", memberInfo.getCompanyId());
                return;
            }

            // 5. 초기 연차 일수 계산 (정책 기반) - 근속연수 고려
            double initialLeave;
            int yearsOfService = Period.between(memberInfo.getJoinDate(), today).getYears();

            if (yearsOfService >= 1) {
                // 1년 이상 근속자: 근속연수 기반 계산
                initialLeave = calculateRegularAnnualLeave(yearsOfService, annualLeavePolicyOpt.get());
                log.info("1년 이상 근속자 연차 계산: memberId={}, 근속년수={}, 부여일수={}일",
                        memberId, yearsOfService, initialLeave);
            } else {
                // 1년 미만 근속자: 전월 근속률 체크 후 월별 발생 계산
                double attendanceRate = calculatePreviousMonthAttendanceRate(memberId, today);
                log.debug("전월 근속률 확인: memberId={}, 근속률={}%", memberId, String.format("%.2f", attendanceRate));

                if (attendanceRate < 80.0) {
                    log.info("근속비율 미달로 연차 부여 안 함: memberId={}, 근속률={}%",
                            memberId, String.format("%.2f", attendanceRate));
                    initialLeave = 0.0;
                } else {
                    initialLeave = calculateFirstYearAccrual(memberInfo.getJoinDate(), today, annualLeavePolicyOpt.get());
                    log.info("1년 미만 근속자 연차 계산: memberId={}, 근속률={}%, 부여일수={}일",
                            memberId, String.format("%.2f", attendanceRate), initialLeave);
                }
            }

            log.info("연차 정책 기반 계산 완료: policyName={}, 근속년수={}, 부여일수={}일",
                    annualLeavePolicyOpt.get().getName(), yearsOfService, initialLeave);

            // 6. MemberBalance 생성
            MemberBalance newBalance = MemberBalance.builder()
                    .memberId(memberId)
                    .companyId(memberInfo.getCompanyId())
                    .balanceTypeCode(PolicyTypeCode.ANNUAL_LEAVE)
                    .year(currentYear)
                    .totalGranted(initialLeave)
                    .totalUsed(0.0)
                    .remaining(initialLeave)
                    .expirationDate(LocalDate.of(currentYear, 12, 31))
                    .isPaid(true)
                    .build();

            memberBalanceRepository.save(newBalance);

            log.info("신규 직원 초기 연차 부여 완료: memberId={}, companyId={}, 부여일수={}일",
                    memberId, memberInfo.getCompanyId(), initialLeave);

        } catch (Exception e) {
            log.error("신규 직원 초기 연차 부여 실패: memberId={}", memberId, e);
            throw e;
        }
    }

    /**
     * 1년 미만 근속자 연차 계산 (근무 개월 수 기반)
     * 입사일부터 기준일까지 근무한 개월 수에 따라 연차 계산
     *
     * @param joinDate 입사일
     * @param referenceDate 기준일 (보통 오늘)
     * @param policy 회사 연차 정책
     * @return 부여할 연차 일수
     */
    private double calculateFirstYearAccrual(LocalDate joinDate, LocalDate referenceDate,
                                              com.crewvy.workforce_service.attendance.entity.Policy policy) {
        var leaveRule = policy.getRuleDetails().getLeaveRule();

        // 신규 구조 우선 사용
        if (leaveRule.getFirstYearRule() != null) {
            var firstYearRule = leaveRule.getFirstYearRule();

            // 월별 연차 발생이 활성화되어 있지 않으면 0일
            if (firstYearRule.getMonthlyAccrualEnabled() == null || !firstYearRule.getMonthlyAccrualEnabled()) {
                log.info("월별 연차 발생 비활성화 (신규): policyName={}", policy.getName());
                return 0.0;
            }

            // 월별 발생 일수 (기본 1일)
            double monthlyDays = firstYearRule.getMonthlyAccrualDays() != null
                ? firstYearRule.getMonthlyAccrualDays()
                : 1.0;

            // 근무 개월 수 계산 (입사 당월 포함)
            int monthsWorked = (int) Period.between(joinDate.withDayOfMonth(1), referenceDate.withDayOfMonth(1)).toTotalMonths() + 1;

            // 최대 발생 일수 제한 (기본 11일)
            int maxAccrual = firstYearRule.getMaxAccrualFirstYear() != null
                ? firstYearRule.getMaxAccrualFirstYear()
                : 11;

            double accrued = Math.min(monthsWorked * monthlyDays, maxAccrual);

            log.info("1년 미만 연차 계산: joinDate={}, 근무개월수={}, 월별발생일수={}, 계산일수={}, 최대일수={}",
                    joinDate, monthsWorked, monthlyDays, accrued, maxAccrual);

            return accrued;
        }

        // 기존 구조로 fallback
        if (leaveRule.getEnableMonthlyAccrual() == null || !leaveRule.getEnableMonthlyAccrual()) {
            log.info("월별 연차 발생 비활성화 (레거시): policyName={}", policy.getName());
            return 0.0;
        }

        double monthlyDays = leaveRule.getMonthlyAccrualDays() != null
            ? leaveRule.getMonthlyAccrualDays()
            : 1.0;

        int monthsWorked = (int) Period.between(joinDate.withDayOfMonth(1), referenceDate.withDayOfMonth(1)).toTotalMonths() + 1;
        int maxAccrual = leaveRule.getFirstYearMaxAccrual() != null
            ? leaveRule.getFirstYearMaxAccrual()
            : 11;

        double accrued = Math.min(monthsWorked * monthlyDays, maxAccrual);

        log.info("1년 미만 연차 계산 (레거시): joinDate={}, 근무개월수={}, 계산일수={}",
                joinDate, monthsWorked, accrued);

        return accrued;
    }

    /**
     * 초기 연차 일수 계산 (정책 기반) - 신규 입사자 전용
     * 회사 연차 정책에 따라 신규 입사자의 초기 연차 계산 (입사 첫 달만)
     *
     * @param joinDate 입사일
     * @param referenceDate 기준일 (보통 오늘)
     * @param policy 회사 연차 정책
     * @return 부여할 연차 일수
     * @deprecated calculateFirstYearAccrual 사용 권장
     */
    @Deprecated
    private double calculateInitialAnnualLeave(LocalDate joinDate, LocalDate referenceDate,
                                              com.crewvy.workforce_service.attendance.entity.Policy policy) {
        var leaveRule = policy.getRuleDetails().getLeaveRule();

        // 신규 구조 우선 사용
        if (leaveRule.getFirstYearRule() != null) {
            var firstYearRule = leaveRule.getFirstYearRule();

            // 월별 연차 발생이 활성화되어 있지 않으면 0일
            if (firstYearRule.getMonthlyAccrualEnabled() == null || !firstYearRule.getMonthlyAccrualEnabled()) {
                log.info("월별 연차 발생 비활성화 (신규): policyName={}", policy.getName());
                return 0.0;
            }

            // 월별 발생 일수 (기본 1일)
            double monthlyDays = firstYearRule.getMonthlyAccrualDays() != null
                ? firstYearRule.getMonthlyAccrualDays()
                : 1.0;

            log.info("초기 연차 계산 (신규): joinDate={}, policyName={}, 월별발생일수={}일",
                    joinDate, policy.getName(), monthlyDays);

            return monthlyDays; // 입사 첫 달 부여
        }

        // 기존 구조로 fallback
        if (leaveRule.getEnableMonthlyAccrual() == null || !leaveRule.getEnableMonthlyAccrual()) {
            log.info("월별 연차 발생 비활성화 (레거시): policyName={}", policy.getName());
            return 0.0;
        }

        double monthlyDays = leaveRule.getMonthlyAccrualDays() != null
            ? leaveRule.getMonthlyAccrualDays()
            : 1.0;

        log.info("초기 연차 계산 (레거시): joinDate={}, policyName={}, 월별발생일수={}일",
                joinDate, policy.getName(), monthlyDays);

        return monthlyDays; // 입사 첫 달 부여
    }

    /**
     * 1년 이상 근로자 연차 계산 (정책 기반)
     * @param yearsOfService 근속 연수
     * @param policy 회사 연차 정책
     * @return 부여할 연차 일수
     */
    private double calculateRegularAnnualLeave(int yearsOfService,
                                              com.crewvy.workforce_service.attendance.entity.Policy policy) {
        var leaveRule = policy.getRuleDetails().getLeaveRule();

        // 신규 구조 우선 사용
        if (leaveRule.getBaseAnnualLeaveForOverOneYear() != null) {
            return calculateWithNewStructure(yearsOfService, leaveRule);
        }

        // 기존 구조로 fallback
        return calculateWithLegacyStructure(yearsOfService, leaveRule);
    }

    /**
     * 신규 구조로 연차 계산 (additionalAnnualLeaveRules 배열 사용)
     */
    private double calculateWithNewStructure(int yearsOfService, com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto leaveRule) {
        // 기본 연차
        double leave = leaveRule.getBaseAnnualLeaveForOverOneYear() != null
                ? leaveRule.getBaseAnnualLeaveForOverOneYear()
                : 15.0;

        // 가산 규칙 적용 (배열 방식)
        if (leaveRule.getAdditionalAnnualLeaveRules() != null && !leaveRule.getAdditionalAnnualLeaveRules().isEmpty()) {
            double additionalTotal = 0.0;
            for (var rule : leaveRule.getAdditionalAnnualLeaveRules()) {
                if (yearsOfService >= rule.getAfterYears()) {
                    additionalTotal += rule.getAdditionalDays();
                    log.debug("가산 적용: {}년차 이상 -> +{}일", rule.getAfterYears(), rule.getAdditionalDays());
                }
            }
            leave += additionalTotal;
            log.debug("총 가산 일수: {}일", additionalTotal);
        }

        // 최대 연차 제한
        int maxDays = leaveRule.getMaximumAnnualLeaveLimit() != null
                ? leaveRule.getMaximumAnnualLeaveLimit()
                : 25;
        leave = Math.min(leave, maxDays);

        log.info("1년 이상 근로자 연차 계산 (신규): 근속년수={}, 발생일수={}일", yearsOfService, leave);
        return leave;
    }

    /**
     * 기존 구조로 연차 계산 (enableAdditionalLeave 방식) - Deprecated 지원
     */
    private double calculateWithLegacyStructure(int yearsOfService, com.crewvy.workforce_service.attendance.dto.rule.LeaveRuleDto leaveRule) {
        String accrualMethod = leaveRule.getAccrualMethod();

        // 1. FIXED: 고정 일수
        if ("FIXED".equals(accrualMethod)) {
            double fixedDays = leaveRule.getDefaultDays() != null ? leaveRule.getDefaultDays() : 15.0;
            log.info("고정 연차 부여 (레거시): 근속년수={}, 부여일수={}일", yearsOfService, fixedDays);
            return fixedDays;
        }

        // 2. 기본 연차 (defaultDays 기본값: 15일)
        double leave = leaveRule.getDefaultDays() != null ? leaveRule.getDefaultDays() : 15.0;

        // 3. 가산 연차 계산 (활성화된 경우)
        if (Boolean.TRUE.equals(leaveRule.getEnableAdditionalLeave())) {
            int startYear = leaveRule.getAdditionalLeaveStartYear() != null
                ? leaveRule.getAdditionalLeaveStartYear() : 3;
            int interval = leaveRule.getAdditionalLeaveInterval() != null
                ? leaveRule.getAdditionalLeaveInterval() : 2;
            double daysPerInterval = leaveRule.getAdditionalLeaveDaysPerInterval() != null
                ? leaveRule.getAdditionalLeaveDaysPerInterval() : 1.0;

            if (yearsOfService >= startYear) {
                int additionalYears = yearsOfService - (startYear - 1);
                double additionalLeave = Math.floor((double) additionalYears / interval) * daysPerInterval;
                leave += additionalLeave;

                log.debug("가산 연차 계산 (레거시): 근속={}, 가산시작={}년, 주기={}년, 가산일수={}, 총가산={}",
                        yearsOfService, startYear, interval, daysPerInterval, additionalLeave);
            }
        }

        // 4. 최대 연차 제한 (기본 25일)
        int maxDays = leaveRule.getMaxAnnualLeaveDays() != null
            ? leaveRule.getMaxAnnualLeaveDays() : 25;
        leave = Math.min(leave, maxDays);

        log.info("1년 이상 근로자 연차 계산 (레거시): 근속년수={}, 발생일수={}일", yearsOfService, leave);
        return leave;
    }

    /**
     * 월별 연차 발생 (1년 미만 근로자용)
     * 매월 1일에 실행하여 정책에 따라 연차 자동 발생
     */
    @Transactional
    public void monthlyAccrualForFirstYearEmployees(UUID companyId, LocalDate referenceDate) {
        log.info("========================================");
        log.info("월별 연차 발생 배치 시작 (1년 미만 근로자): companyId={}, referenceDate={}",
                companyId, referenceDate);
        log.info("========================================");

        try {
            // 1. 연차 정책 조회
            Optional<com.crewvy.workforce_service.attendance.entity.Policy> annualLeavePolicyOpt =
                    policyRepository.findActiveAnnualLeavePolicy(companyId);

            if (annualLeavePolicyOpt.isEmpty()) {
                log.warn("연차 정책이 없어 배치 중단: companyId={}", companyId);
                return;
            }

            com.crewvy.workforce_service.attendance.entity.Policy policy = annualLeavePolicyOpt.get();
            var leaveRule = policy.getRuleDetails().getLeaveRule();
            log.info("연차 정책 확인: policyName={}", policy.getName());

            // 2. firstYearRule 확인 (신규 구조 우선)
            boolean monthlyAccrualEnabled;
            double monthlyAccrualDays;
            int maxAccrual;

            if (leaveRule.getFirstYearRule() != null) {
                var firstYearRule = leaveRule.getFirstYearRule();
                monthlyAccrualEnabled = firstYearRule.getMonthlyAccrualEnabled() != null
                        && firstYearRule.getMonthlyAccrualEnabled();
                monthlyAccrualDays = firstYearRule.getMonthlyAccrualDays() != null
                        ? firstYearRule.getMonthlyAccrualDays() : 1.0;
                maxAccrual = firstYearRule.getMaxAccrualFirstYear() != null
                        ? firstYearRule.getMaxAccrualFirstYear() : 11;
                log.info("신규 구조 적용: 월별발생={}, 발생일수={}, 최대={}일", monthlyAccrualEnabled, monthlyAccrualDays, maxAccrual);
            } else {
                // 레거시 구조 fallback
                monthlyAccrualEnabled = leaveRule.getEnableMonthlyAccrual() != null
                        && leaveRule.getEnableMonthlyAccrual();
                monthlyAccrualDays = leaveRule.getMonthlyAccrualDays() != null
                        ? leaveRule.getMonthlyAccrualDays() : 1.0;
                maxAccrual = leaveRule.getFirstYearMaxAccrual() != null
                        ? leaveRule.getFirstYearMaxAccrual() : 11;
                log.info("레거시 구조 적용: 월별발생={}, 발생일수={}, 최대={}일", monthlyAccrualEnabled, monthlyAccrualDays, maxAccrual);
            }

            if (!monthlyAccrualEnabled) {
                log.info("월별 연차 발생이 비활성화되어 있음: policyName={}", policy.getName());
                return;
            }

            // 3. 내부 전용 API로 직원 조회
            List<MemberEmploymentInfoDto> members = memberClient
                    .getEmploymentInfoInternal(companyId)
                    .getData();

            if (members == null || members.isEmpty()) {
                log.info("월별 연차 발생 대상 없음: companyId={}", companyId);
                return;
            }

            // 4. 1년 미만 근로자 필터링
            List<MemberEmploymentInfoDto> firstYearMembers = members.stream()
                    .filter(m -> "MS001".equals(m.getMemberStatus()) && m.getJoinDate() != null) // MS001 = WORKING (재직)
                    .filter(m -> Period.between(m.getJoinDate(), referenceDate).getYears() < 1)
                    .toList();

            if (firstYearMembers.isEmpty()) {
                log.info("1년 미만 근로자 없음: companyId={}", companyId);
                return;
            }

            log.info(">>> 처리 대상: {}명", firstYearMembers.size());

            // 5. 기존 잔액 조회
            List<UUID> firstYearMemberIds = firstYearMembers.stream()
                    .map(MemberEmploymentInfoDto::getMemberId)
                    .toList();

            int currentYear = referenceDate.getYear();
            List<MemberBalance> existingBalances = memberBalanceRepository
                    .findByMemberIdInAndBalanceTypeCodeAndYear(
                            firstYearMemberIds,
                            PolicyTypeCode.ANNUAL_LEAVE,
                            currentYear
                    );

            Map<UUID, MemberBalance> balanceMap = existingBalances.stream()
                    .collect(Collectors.toMap(MemberBalance::getMemberId, java.util.function.Function.identity()));

            // 6. 월별 연차 발생 처리
            List<MemberBalance> balancesToSave = new ArrayList<>();
            int processedCount = 0;
            int skippedByAttendanceRate = 0;

            for (MemberEmploymentInfoDto member : firstYearMembers) {
                // 전월 근속비율 체크 (80% 미만이면 스킵)
                double attendanceRate = calculatePreviousMonthAttendanceRate(member.getMemberId(), referenceDate);
                if (attendanceRate < 80.0) {
                    log.info("근속비율 미달로 월별 연차 발생 스킵: memberId={}, 근속비율={}%",
                            member.getMemberId(), String.format("%.2f", attendanceRate));
                    skippedByAttendanceRate++;
                    continue;
                }

                MemberBalance balance = balanceMap.get(member.getMemberId());

                if (balance != null) {
                    // 이미 잔액이 있는 경우 - 추가 발생
                    if (balance.getTotalGranted() < maxAccrual) {
                        double newTotal = Math.min(balance.getTotalGranted() + monthlyAccrualDays, maxAccrual);
                        double newRemaining = newTotal - balance.getTotalUsed();

                        MemberBalance updated = MemberBalance.builder()
                                .id(balance.getId())
                                .memberId(balance.getMemberId())
                                .companyId(balance.getCompanyId())
                                .balanceTypeCode(balance.getBalanceTypeCode())
                                .year(balance.getYear())
                                .totalGranted(newTotal)
                                .totalUsed(balance.getTotalUsed())
                                .remaining(newRemaining)
                                .expirationDate(balance.getExpirationDate())
                                .isPaid(balance.getIsPaid())
                                .build();
                        balancesToSave.add(updated);
                        processedCount++;
                        log.info("월별 연차 추가 발생: memberId={}, 기존={}, 신규={}, 증가={}일",
                                member.getMemberId(), balance.getTotalGranted(), newTotal, monthlyAccrualDays);
                    } else {
                        log.debug("최대 연차 도달로 스킵: memberId={}, 현재={}일", member.getMemberId(), balance.getTotalGranted());
                    }
                } else {
                    // 첫 월별 연차 발생
                    MemberBalance newBalance = MemberBalance.builder()
                            .memberId(member.getMemberId())
                            .companyId(member.getCompanyId())
                            .balanceTypeCode(PolicyTypeCode.ANNUAL_LEAVE)
                            .year(currentYear)
                            .totalGranted(monthlyAccrualDays)
                            .totalUsed(0.0)
                            .remaining(monthlyAccrualDays)
                            .expirationDate(LocalDate.of(currentYear, 12, 31))
                            .isPaid(true)
                            .build();
                    balancesToSave.add(newBalance);
                    processedCount++;
                    log.info("첫 월별 연차 발생: memberId={}, 발생일수={}일", member.getMemberId(), monthlyAccrualDays);
                }
            }

            // 7. 일괄 저장
            if (!balancesToSave.isEmpty()) {
                memberBalanceRepository.saveAll(balancesToSave);
            }

            log.info("========================================");
            log.info("월별 연차 발생 완료: 처리건수={}/{}", processedCount, firstYearMembers.size());
            log.info("근속비율 미달로 스킵: {}건 (80% 미만)", skippedByAttendanceRate);
            log.info("========================================");

        } catch (Exception e) {
            log.error("월별 연차 발생 중 오류: companyId={}", companyId, e);
            throw e;
        }
    }

    /**
     * 전월 근속비율 계산
     * @param memberId 직원 ID
     * @param referenceDate 기준 날짜 (보통 현재 날짜)
     * @return 전월 근속비율 (0.0 ~ 100.0)
     */
    public double calculatePreviousMonthAttendanceRate(UUID memberId, LocalDate referenceDate) {
        // 전월의 첫날과 마지막날 계산
        LocalDate previousMonthStart = referenceDate.minusMonths(1).withDayOfMonth(1);
        LocalDate previousMonthEnd = previousMonthStart.plusMonths(1).minusDays(1);

        // 전월의 총 근무일수 계산 (주말 제외)
        long totalWorkDays = 0;
        LocalDate current = previousMonthStart;
        while (!current.isAfter(previousMonthEnd)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                totalWorkDays++;
            }
            current = current.plusDays(1);
        }

        if (totalWorkDays == 0) {
            return 0.0;
        }

        // 전월의 DailyAttendance 조회
        List<DailyAttendance> attendances = dailyAttendanceRepository
                .findByMemberIdAndAttendanceDateBetween(memberId, previousMonthStart, previousMonthEnd);

        // 정상 출근일수 계산 (NORMAL_WORK, BUSINESS_TRIP, 휴가 등 모두 포함)
        long actualAttendanceDays = attendances.stream()
                .filter(attendance -> {
                    AttendanceStatus status = attendance.getStatus();
                    // 결근이 아닌 모든 상태를 출근으로 인정
                    return status != AttendanceStatus.ABSENT;
                })
                .count();

        // 근속비율 계산 (%)
        double attendanceRate = (double) actualAttendanceDays / totalWorkDays * 100.0;

        log.debug("근속비율 계산: memberId={}, 전월={}-{}, 총근무일={}일, 실출근={}일, 비율={}%",
                memberId, previousMonthStart, previousMonthEnd, totalWorkDays, actualAttendanceDays,
                String.format("%.2f", attendanceRate));

        return attendanceRate;
    }

    /**
     * 회사 전체 직원에게 초기 연차 부여 (정책 할당 시 호출)
     * 모든 재직 중인 직원에 대해 즉시 연차 발생 처리
     */
    @Transactional
    public void grantInitialAnnualLeaveForAllMembers(UUID companyId, LocalDate referenceDate) {
        log.info("회사 전체 직원 초기 연차 부여 시작: companyId={}, referenceDate={}", companyId, referenceDate);

        try {
            // 1. 회사의 모든 직원 조회 (내부 전용 API 사용)
            List<MemberEmploymentInfoDto> members = memberClient
                    .getEmploymentInfoInternal(companyId)
                    .getData();

            if (members == null || members.isEmpty()) {
                log.info("초기 연차 부여 대상 직원 없음: companyId={}", companyId);
                return;
            }

            // 2. 재직 중인 직원만 필터링
            List<MemberEmploymentInfoDto> activeMembers = members.stream()
                    .filter(m -> "MS001".equals(m.getMemberStatus()) && m.getJoinDate() != null) // MS001 = WORKING (재직)
                    .toList();

            if (activeMembers.isEmpty()) {
                log.info("재직 중인 직원 없음: companyId={}", companyId);
                return;
            }

            log.info(">>> 초기 연차 부여 대상: {}명", activeMembers.size());

            // 3. 각 직원에게 초기 연차 부여
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (MemberEmploymentInfoDto member : activeMembers) {
                try {
                    grantInitialAnnualLeave(member.getMemberId());
                    successCount++;
                } catch (Exception e) {
                    // 이미 존재하는 경우 스킵
                    if (e.getMessage() != null && e.getMessage().contains("이미")) {
                        skipCount++;
                    } else {
                        log.error("직원 연차 부여 실패: memberId={}", member.getMemberId(), e);
                        failCount++;
                    }
                }
            }

            log.info("회사 전체 직원 초기 연차 부여 완료: 성공={}, 스킵={}, 실패={}", successCount, skipCount, failCount);

        } catch (Exception e) {
            log.error("회사 전체 직원 초기 연차 부여 중 오류: companyId={}", companyId, e);
            throw e;
        }
    }
}
