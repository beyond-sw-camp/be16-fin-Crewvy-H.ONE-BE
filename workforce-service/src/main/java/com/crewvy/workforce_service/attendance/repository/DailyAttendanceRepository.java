package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, UUID> {
    Optional<DailyAttendance> findByMemberIdAndAttendanceDate(UUID memberId, LocalDate attendanceDate);

    /**
     * 기간별 전체 직원 일일 근태 조회
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 기간 내 모든 직원의 일일 근태 목록
     */
    @Query("SELECT da FROM DailyAttendance da " +
            "WHERE da.attendanceDate BETWEEN :startDate AND :endDate " +
            "ORDER BY da.memberId, da.attendanceDate")
    List<DailyAttendance> findAllByDateRange(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * 기간별 회사 소속 직원 일일 근태 조회 (Multi-tenant 보안)
     * @param companyId 회사 ID
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 해당 회사의 기간 내 직원 일일 근태 목록
     */
    @Query("SELECT da FROM DailyAttendance da " +
            "WHERE da.companyId = :companyId " +
            "AND da.attendanceDate BETWEEN :startDate AND :endDate " +
            "ORDER BY da.memberId, da.attendanceDate")
    List<DailyAttendance> findAllByDateRangeAndCompany(
            @Param("companyId") UUID companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
