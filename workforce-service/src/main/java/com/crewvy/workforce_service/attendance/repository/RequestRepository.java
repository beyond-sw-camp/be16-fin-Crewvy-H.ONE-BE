package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.entity.Request;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {

    @Query("SELECT count(r) > 0 FROM Request r WHERE r.memberId = :memberId AND r.deviceId = :deviceId AND r.deviceType = :deviceType AND r.status = :status")
    boolean existsApprovedDevice(@Param("memberId") UUID memberId,
                                 @Param("deviceId") String deviceId,
                                 @Param("deviceType") DeviceType deviceType,
                                 @Param("status") RequestStatus status);

    /**
     * 내 휴가 신청 목록 조회 (페이징)
     */
    Page<Request> findByMemberId(UUID memberId, Pageable pageable);

    /**
     * 중복 신청 확인
     * 날짜 범위가 겹치는 PENDING 상태의 신청이 있는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Request r " +
           "WHERE r.memberId = :memberId " +
           "AND r.status = :status " +
           "AND ((r.startDateTime BETWEEN :startDateTime AND :endDateTime) " +
           "OR (r.endDateTime BETWEEN :startDateTime AND :endDateTime) " +
           "OR (r.startDateTime <= :startDateTime AND r.endDateTime >= :endDateTime))")
    boolean existsByMemberIdAndDateRangeAndStatus(
            @Param("memberId") UUID memberId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("status") RequestStatus status
    );

    /**
     * 디바이스 등록 신청 목록 조회 (내 디바이스만, 디바이스 필드가 null이 아닌 것)  
     */
    @Query("SELECT r FROM Request r WHERE r.memberId = :memberId AND r.deviceId IS NOT NULL ORDER BY r.createdAt DESC")
    Page<Request> findDeviceRequestsByMemberId(@Param("memberId") UUID memberId, Pageable pageable);

    /**
     * 승인 대기 중인 디바이스 등록 신청 목록 (관리자용)
     */
    @Query("SELECT r FROM Request r WHERE r.deviceId IS NOT NULL AND r.status = :status ORDER BY r.createdAt DESC")
    Page<Request> findDeviceRequestsByStatus(@Param("status") RequestStatus status, Pageable pageable);

    /**
     * 내 휴가 신청 목록 조회 (페이징) - 정책이 있는(휴가) 요청만
     */
    @Query("SELECT r FROM Request r WHERE r.memberId = :memberId AND r.policy IS NOT NULL ORDER BY r.createdAt DESC")
    Page<Request> findLeaveRequestsByMemberId(@Param("memberId") UUID memberId, Pageable pageable);

    /**
     * 특정 디바이스 ID로 이미 등록된 신청이 있는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Request r " +
           "WHERE r.memberId = :memberId AND r.deviceId = :deviceId AND r.deviceType = :deviceType")
    boolean existsByMemberIdAndDeviceIdAndDeviceType(
            @Param("memberId") UUID memberId,
            @Param("deviceId") String deviceId,
            @Param("deviceType") DeviceType deviceType
    );

    @Query("SELECT SUM(r.deductionDays) FROM Request r " +
           "WHERE r.memberId = :memberId " +
           "AND r.policy.id = :policyId " +
           "AND r.status = :status " +
           "AND r.startDateTime >= :periodStart AND r.endDateTime <= :periodEnd")
    Optional<Double> sumDeductionDaysByMemberIdAndPolicyIdAndStatusInDateRange(
            @Param("memberId") UUID memberId,
            @Param("policyId") UUID policyId,
            @Param("status") RequestStatus status,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    /**
     * 주간 연장근무 신청 목록 조회 (근로기준법 제53조 - 주 12시간 한도 검증용)
     * OVERTIME, NIGHT_WORK, HOLIDAY_WORK 타입의 승인된 신청 조회
     */
    @Query("SELECT r FROM Request r " +
           "WHERE r.memberId = :memberId " +
           "AND r.policy.policyType.typeCode IN ('OVERTIME', 'NIGHT_WORK', 'HOLIDAY_WORK') " +
           "AND r.status IN ('APPROVED', 'PENDING') " +
           "AND r.startDateTime >= :weekStart " +
           "AND r.startDateTime < :weekEnd")
    java.util.List<Request> findApprovedOvertimeRequestsInWeek(
            @Param("memberId") UUID memberId,
            @Param("weekStart") LocalDateTime weekStart,
            @Param("weekEnd") LocalDateTime weekEnd
    );

    /**
     * 특정 정책으로 승인된 신청 횟수 조회 (분할 사용 횟수 체크용)
     */
    @Query("SELECT COUNT(r) FROM Request r " +
           "WHERE r.memberId = :memberId " +
           "AND r.policy.id = :policyId " +
           "AND r.status = :status " +
           "AND r.startDateTime >= :yearStart " +
           "AND r.startDateTime <= :yearEnd")
    int countByMemberIdAndPolicyIdAndStatusAndStartDateTimeBetween(
            @Param("memberId") UUID memberId,
            @Param("policyId") UUID policyId,
            @Param("status") RequestStatus status,
            @Param("yearStart") LocalDateTime yearStart,
            @Param("yearEnd") LocalDateTime yearEnd
    );

    /**
     * 중복 신청 확인 (동시성 제어용)
     * 같은 사용자가 같은 정책으로 날짜가 겹치는 PENDING 또는 APPROVED 신청이 있는지 확인
     *
     * @param memberId 사용자 ID
     * @param policyId 정책 ID
     * @param startDateTime 신청 시작 일시
     * @param endDateTime 신청 종료 일시
     * @return 중복 신청이 있으면 true, 없으면 false
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Request r " +
           "WHERE r.memberId = :memberId " +
           "AND r.policy.id = :policyId " +
           "AND r.status IN ('PENDING', 'APPROVED') " +
           "AND ((r.startDateTime <= :endDateTime AND r.endDateTime >= :startDateTime))")
    boolean existsDuplicateRequest(
            @Param("memberId") UUID memberId,
            @Param("policyId") UUID policyId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
