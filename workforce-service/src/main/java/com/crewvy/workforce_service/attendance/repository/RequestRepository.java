package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.entity.Request;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
           "AND ((r.startAt BETWEEN :startAt AND :endAt) " +
           "OR (r.endAt BETWEEN :startAt AND :endAt) " +
           "OR (r.startAt <= :startAt AND r.endAt >= :endAt))")
    boolean existsByMemberIdAndDateRangeAndStatus(
            @Param("memberId") UUID memberId,
            @Param("startAt") LocalDate startAt,
            @Param("endAt") LocalDate endAt,
            @Param("status") RequestStatus status
    );
}
