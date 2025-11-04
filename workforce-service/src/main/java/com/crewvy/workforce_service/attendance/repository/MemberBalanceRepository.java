package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberBalanceRepository extends JpaRepository<MemberBalance, UUID> {
    /**
     * 연도별 전체 직원 잔여 일수 조회
     * @param year 조회 연도
     * @return 해당 연도의 전체 직원 잔여 일수 목록
     */
    @Query("SELECT mb FROM MemberBalance mb " +
            "WHERE mb.year = :year " +
            "ORDER BY mb.memberId")
    List<MemberBalance> findAllByYear(@Param("year") Integer year);

    /**
     * 연도별 회사 소속 직원 잔여 일수 조회 (Multi-tenant 보안)
     * @param companyId 회사 ID
     * @param year 조회 연도
     * @return 해당 회사의 해당 연도 직원 잔여 일수 목록
     */
    @Query("SELECT mb FROM MemberBalance mb " +
            "WHERE mb.companyId = :companyId " +
            "AND mb.year = :year " +
            "ORDER BY mb.memberId")
    List<MemberBalance> findAllByYearAndCompany(
            @Param("companyId") UUID companyId,
            @Param("year") Integer year);
    // findAllByYearAndCompany 아래 메서드로 바꾸면 @Query 안써도 됨
    List<MemberBalance> findAllByCompanyIdAndYearOrderByMemberIdAsc(
            UUID companyId, Integer year);

    /**
     * 특정 직원의 특정 연도 특정 타입의 잔여 일수 조회
     * @param memberId 직원 ID
     * @param balanceTypeCode 잔여 일수 타입 (PolicyTypeCode)
     * @param year 조회 연도
     * @return 해당 직원의 해당 연도 해당 타입 잔여 일수
     */
    Optional<MemberBalance> findByMemberIdAndBalanceTypeCodeAndYear(
            UUID memberId,
            PolicyTypeCode balanceTypeCode,
            Integer year
    );

    /**
     * 특정 직원의 특정 연도 모든 휴가 정책 잔액 조회
     */
    List<MemberBalance> findAllByMemberIdAndYear(UUID memberId, Integer year);

    List<MemberBalance> findAllByMemberIdInAndYear(List<UUID> memberIds, int year);

    List<MemberBalance> findByMemberIdInAndBalanceTypeCodeAndYear(List<UUID> memberIds, PolicyTypeCode balanceTypeCode, int year);
}
