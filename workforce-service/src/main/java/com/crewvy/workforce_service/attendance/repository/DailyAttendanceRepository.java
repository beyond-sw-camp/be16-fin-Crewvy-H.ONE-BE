package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<DailyAttendance> findAllByMemberIdInAndAttendanceDateBetween(List<UUID> memberIds, LocalDate startDate, LocalDate endDate);

    Optional<DailyAttendance> findFirstByMemberIdOrderByAttendanceDateDesc(UUID memberId);

    @Query("SELECT da FROM DailyAttendance da " +
           "WHERE da.attendanceDate = :date " +
           "AND da.firstClockIn IS NOT NULL AND da.lastClockOut IS NULL " +
           "AND da.status IN (com.crewvy.workforce_service.attendance.constant.AttendanceStatus.NORMAL_WORK, com.crewvy.workforce_service.attendance.constant.AttendanceStatus.BUSINESS_TRIP)")
    Page<DailyAttendance> findIncompleteAttendances(@Param("date") LocalDate date, Pageable pageable);

    /**
     * 월별 지각 횟수 조회 (허용 시간 내 포함)
     * lateMinutes > 0인 경우 모두 카운트
     */
    @Query("SELECT COUNT(da) FROM DailyAttendance da " +
           "WHERE da.memberId = :memberId " +
           "AND da.attendanceDate BETWEEN :startDate AND :endDate " +
           "AND da.lateMinutes > 0")
    int countMonthlyLateness(@Param("memberId") UUID memberId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    /**
     * 월별 조퇴 횟수 조회 (허용 시간 내 포함)
     * earlyLeaveMinutes > 0인 경우 모두 카운트
     */
    @Query("SELECT COUNT(da) FROM DailyAttendance da " +
           "WHERE da.memberId = :memberId " +
           "AND da.attendanceDate BETWEEN :startDate AND :endDate " +
           "AND da.earlyLeaveMinutes > 0")
    int countMonthlyEarlyLeave(@Param("memberId") UUID memberId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);
}
