package com.crewvy.workforce_service.attendance.service;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import com.crewvy.workforce_service.attendance.repository.MemberBalanceRepository;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberEmploymentInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    // 시스템 관리자 UUID (배치 실행용)
    private static final UUID SYSTEM_ADMIN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    /**
     * 회사의 모든 활성 직원에 대해 연차 자동 발생
     * @param companyId 회사 ID
     * @param referenceDate 기준 날짜 (보통 현재 날짜)
     */
    @Transactional
    public void accrueAnnualLeaveForCompany(UUID companyId, LocalDate referenceDate) {
        log.info("연차 자동 발생 시작: companyId={}, referenceDate={}", companyId, referenceDate);

        try {
            List<MemberEmploymentInfoDto> members = memberClient
                    .getEmploymentInfo(SYSTEM_ADMIN_UUID, companyId)
                    .getData();

            if (members == null || members.isEmpty()) {
                log.info("연차 발생 대상 직원 없음: companyId={}", companyId);
                return;
            }

            List<MemberEmploymentInfoDto> regularMembers = members.stream()
                .filter(m -> "WORKING".equals(m.getMemberStatus()) && m.getJoinDate() != null)
                .filter(m -> Period.between(m.getJoinDate(), referenceDate).getYears() >= 1)
                .toList();

            if (regularMembers.isEmpty()) {
                log.info("1년 이상 근속 직원 없음: companyId={}", companyId);
                return;
            }

            List<UUID> regularMemberIds = regularMembers.stream()
                    .map(MemberEmploymentInfoDto::getMemberId)
                    .toList();
            
            int currentYear = referenceDate.getYear();
            List<MemberBalance> existingBalances = memberBalanceRepository
                    .findByMemberIdInAndBalanceTypeCodeAndYear(
                            regularMemberIds,
                            PolicyTypeCode.ANNUAL_LEAVE,
                            currentYear
                    );

            Map<UUID, MemberBalance> balanceMap = existingBalances.stream()
                    .collect(Collectors.toMap(MemberBalance::getMemberId, java.util.function.Function.identity()));

            List<MemberBalance> balancesToSave = new ArrayList<>();
            int successCount = 0;

            for (MemberEmploymentInfoDto member : regularMembers) {
                if (!balanceMap.containsKey(member.getMemberId())) {
                    accrueAnnualLeaveForRegularMember(member, referenceDate, balancesToSave);
                    successCount++;
                }
            }

            if (!balancesToSave.isEmpty()) {
                memberBalanceRepository.saveAll(balancesToSave);
            }

            log.info("연차 자동 발생 완료: 성공={}, 스킵={}", successCount, regularMembers.size() - successCount);

        } catch (Exception e) {
            log.error("연차 자동 발생 중 오류 발생: companyId={}", companyId, e);
            throw e;
        }
    }

    private void accrueAnnualLeaveForRegularMember(MemberEmploymentInfoDto member, LocalDate referenceDate, List<MemberBalance> balancesToSave) {
        int yearsOfService = Period.between(member.getJoinDate(), referenceDate).getYears();
        double annualLeaveToGrant = calculateRegularAnnualLeave(yearsOfService);
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
     * 1년 이상 근로자 연차 계산 (근로기준법 제60조 제1항)
     * 기본 15일 + 2년마다 1일 추가 (최대 25일)
     */
    private double calculateRegularAnnualLeave(int yearsOfService) {
        // 기본 15일
        double leave = 15.0;

        // 2년 근속마다 1일 추가
        if (yearsOfService >= 2) {
            int additionalYears = yearsOfService - 1; // 1년차는 이미 15일
            double additionalLeave = Math.floor(additionalYears / 2.0); // 2년마다 1일
            leave += additionalLeave;
        }

        // 최대 25일
        leave = Math.min(leave, 25.0);

        log.debug("1년 이상 근로자 연차: 근속년수={}, 발생일수={}", yearsOfService, leave);

        return leave;
    }

    /**
     * 월별 연차 발생 (1년 미만 근로자용)
     * 매월 1일에 실행하여 1개월 개근 시 1일 추가 발생
     */
    @Transactional
    public void monthlyAccrualForFirstYearEmployees(UUID companyId, LocalDate referenceDate) {
        log.info("월별 연차 발생 시작 (1년 미만 근로자): companyId={}, referenceDate={}",
                companyId, referenceDate);

        try {
            List<MemberEmploymentInfoDto> members = memberClient
                    .getEmploymentInfo(SYSTEM_ADMIN_UUID, companyId)
                    .getData();

            if (members == null || members.isEmpty()) {
                log.info("월별 연차 발생 대상 없음: companyId={}", companyId);
                return;
            }

            List<MemberEmploymentInfoDto> firstYearMembers = members.stream()
                    .filter(m -> "WORKING".equals(m.getMemberStatus()) && m.getJoinDate() != null)
                    .filter(m -> Period.between(m.getJoinDate(), referenceDate).getYears() < 1)
                    .toList();

            if (firstYearMembers.isEmpty()) {
                log.info("1년 미만 근로자 없음: companyId={}", companyId);
                return;
            }

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

            List<MemberBalance> balancesToSave = new ArrayList<>();
            int processedCount = 0;

            for (MemberEmploymentInfoDto member : firstYearMembers) {
                MemberBalance balance = balanceMap.get(member.getMemberId());

                if (balance != null) {
                    if (balance.getTotalGranted() < 11.0) {
                        double newTotal = Math.min(balance.getTotalGranted() + 1.0, 11.0);
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
                        log.info("월별 연차 추가 발생: memberId={}, 기존={}, 신규={}",
                                member.getMemberId(), balance.getTotalGranted(), newTotal);
                    }
                } else {
                    MemberBalance newBalance = MemberBalance.builder()
                            .memberId(member.getMemberId())
                            .companyId(member.getCompanyId())
                            .balanceTypeCode(PolicyTypeCode.ANNUAL_LEAVE)
                            .year(currentYear)
                            .totalGranted(1.0)
                            .totalUsed(0.0)
                            .remaining(1.0)
                            .expirationDate(LocalDate.of(currentYear, 12, 31))
                            .isPaid(true)
                            .build();
                    balancesToSave.add(newBalance);
                    processedCount++;
                    log.info("첫 월별 연차 발생: memberId={}", member.getMemberId());
                }
            }

            if (!balancesToSave.isEmpty()) {
                memberBalanceRepository.saveAll(balancesToSave);
            }

            log.info("월별 연차 발생 완료: 처리건수={}", processedCount);

        } catch (Exception e) {
            log.error("월별 연차 발생 중 오류: companyId={}", companyId, e);
            throw e;
        }
    }
}
