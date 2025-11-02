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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
            // 1. 회사의 모든 직원 고용 정보 조회
            List<MemberEmploymentInfoDto> members = memberClient
                    .getEmploymentInfo(SYSTEM_ADMIN_UUID, companyId)
                    .getData();

            if (members == null || members.isEmpty()) {
                log.info("연차 발생 대상 직원 없음: companyId={}", companyId);
                return;
            }

            log.info("연차 발생 대상 직원 수: {}", members.size());

            // 2. 각 직원별 연차 발생 처리
            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;

            for (MemberEmploymentInfoDto member : members) {
                try {
                    // 재직 중인 직원만 처리
                    if (!"WORKING".equals(member.getMemberStatus())) {
                        log.debug("재직 중이 아닌 직원 스킵: memberId={}, status={}",
                                member.getMemberId(), member.getMemberStatus());
                        skipCount++;
                        continue;
                    }

                    // 입사일이 없으면 스킵
                    if (member.getJoinDate() == null) {
                        log.warn("입사일이 없는 직원 스킵: memberId={}", member.getMemberId());
                        skipCount++;
                        continue;
                    }

                    // 연차 발생 처리
                    accrueAnnualLeaveForMember(member, referenceDate);
                    successCount++;

                } catch (Exception e) {
                    log.error("직원 연차 발생 실패: memberId={}", member.getMemberId(), e);
                    errorCount++;
                }
            }

            log.info("연차 자동 발생 완료: 성공={}, 스킵={}, 실패={}", successCount, skipCount, errorCount);

        } catch (Exception e) {
            log.error("연차 자동 발생 중 오류 발생: companyId={}", companyId, e);
            throw e;
        }
    }

    /**
     * 개별 직원의 연차 발생 처리
     * 근로기준법 제60조:
     * - 1년 미만: 1개월 개근 시 1일 (최대 11일)
     * - 1년 이상: 15일 + (2년마다 1일, 최대 25일)
     */
    @Transactional
    public void accrueAnnualLeaveForMember(MemberEmploymentInfoDto member, LocalDate referenceDate) {
        // 1. 근속 기간 계산
        Period tenure = Period.between(member.getJoinDate(), referenceDate);
        int yearsOfService = tenure.getYears();
        int monthsOfService = tenure.getMonths();

        log.debug("연차 발생 계산: memberId={}, 입사일={}, 근속={}년 {}개월",
                member.getMemberId(), member.getJoinDate(), yearsOfService, monthsOfService);

        // 2. 현재 연도 잔액 조회
        int currentYear = referenceDate.getYear();
        Optional<MemberBalance> existingBalance = memberBalanceRepository
                .findByMemberIdAndBalanceTypeCodeAndYear(
                        member.getMemberId(),
                        PolicyTypeCode.ANNUAL_LEAVE,
                        currentYear
                );

        double annualLeaveToGrant;

        // 3. 연차 발생 일수 계산
        if (yearsOfService < 1) {
            // 1년 미만: 매월 1일 발생 (최대 11일)
            annualLeaveToGrant = calculateFirstYearAnnualLeave(member.getJoinDate(), referenceDate);
        } else {
            // 1년 이상: 15일 + 추가 일수
            annualLeaveToGrant = calculateRegularAnnualLeave(yearsOfService);
        }

        log.debug("계산된 연차 일수: memberId={}, 발생일수={}", member.getMemberId(), annualLeaveToGrant);

        // 4. MemberBalance 생성 또는 업데이트
        if (existingBalance.isPresent()) {
            // 이미 존재하면 업데이트는 하지 않음 (중복 발생 방지)
            log.debug("이미 연차가 발생된 직원: memberId={}, year={}", member.getMemberId(), currentYear);
        } else {
            // 새로운 연차 발생
            MemberBalance newBalance = MemberBalance.builder()
                    .memberId(member.getMemberId())
                    .companyId(member.getCompanyId())
                    .balanceTypeCode(PolicyTypeCode.ANNUAL_LEAVE)
                    .year(currentYear)
                    .totalGranted(annualLeaveToGrant)
                    .totalUsed(0.0)
                    .remaining(annualLeaveToGrant)
                    .expirationDate(LocalDate.of(currentYear, 12, 31)) // 당해년도 말
                    .isPaid(true)
                    .build();

            memberBalanceRepository.save(newBalance);
            log.info("연차 발생 완료: memberId={}, year={}, 발생일수={}",
                    member.getMemberId(), currentYear, annualLeaveToGrant);
        }
    }

    /**
     * 1년 미만 근로자 연차 계산 (근로기준법 제60조 제2항)
     * 1개월 개근 시 1일, 최대 11일
     */
    private double calculateFirstYearAnnualLeave(LocalDate joinDate, LocalDate referenceDate) {
        Period tenure = Period.between(joinDate, referenceDate);
        int totalMonths = tenure.getYears() * 12 + tenure.getMonths();

        // 1개월 근무마다 1일 발생, 최대 11일
        double leave = Math.min(totalMonths, 11);

        log.debug("1년 미만 근로자 연차: 입사일={}, 기준일={}, 근속월수={}, 발생일수={}",
                joinDate, referenceDate, totalMonths, leave);

        return leave;
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

            int processedCount = 0;

            for (MemberEmploymentInfoDto member : members) {
                if (!"WORKING".equals(member.getMemberStatus()) || member.getJoinDate() == null) {
                    continue;
                }

                // 근속 기간 계산
                Period tenure = Period.between(member.getJoinDate(), referenceDate);
                if (tenure.getYears() >= 1) {
                    continue; // 1년 이상 근로자는 스킵
                }

                // 현재 연도 잔액 조회
                int currentYear = referenceDate.getYear();
                Optional<MemberBalance> balance = memberBalanceRepository
                        .findByMemberIdAndBalanceTypeCodeAndYear(
                                member.getMemberId(),
                                PolicyTypeCode.ANNUAL_LEAVE,
                                currentYear
                        );

                if (balance.isPresent()) {
                    // 월별 1일 추가 (최대 11일까지)
                    MemberBalance memberBalance = balance.get();
                    if (memberBalance.getTotalGranted() < 11.0) {
                        double newTotal = Math.min(memberBalance.getTotalGranted() + 1.0, 11.0);
                        double newRemaining = newTotal - memberBalance.getTotalUsed();

                        // 업데이트를 위해 새 엔티티 생성 (불변 엔티티 패턴)
                        MemberBalance updated = MemberBalance.builder()
                                .id(memberBalance.getId())
                                .memberId(memberBalance.getMemberId())
                                .companyId(memberBalance.getCompanyId())
                                .balanceTypeCode(memberBalance.getBalanceTypeCode())
                                .year(memberBalance.getYear())
                                .totalGranted(newTotal)
                                .totalUsed(memberBalance.getTotalUsed())
                                .remaining(newRemaining)
                                .expirationDate(memberBalance.getExpirationDate())
                                .isPaid(memberBalance.getIsPaid())
                                .build();

                        memberBalanceRepository.save(updated);
                        processedCount++;

                        log.info("월별 연차 추가 발생: memberId={}, 기존={}, 신규={}",
                                member.getMemberId(), memberBalance.getTotalGranted(), newTotal);
                    }
                } else {
                    // 잔액이 없으면 1일 발생
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

                    memberBalanceRepository.save(newBalance);
                    processedCount++;

                    log.info("첫 월별 연차 발생: memberId={}", member.getMemberId());
                }
            }

            log.info("월별 연차 발생 완료: 처리건수={}", processedCount);

        } catch (Exception e) {
            log.error("월별 연차 발생 중 오류: companyId={}", companyId, e);
            throw e;
        }
    }
}
