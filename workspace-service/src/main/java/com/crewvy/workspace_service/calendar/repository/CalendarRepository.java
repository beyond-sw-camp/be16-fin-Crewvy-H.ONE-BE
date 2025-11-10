package com.crewvy.workspace_service.calendar.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.calendar.entity.Calendar;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, UUID> {
    /**
     * (신규) 특정 기간[periodStart, periodEnd]과 겹치는(Overlap) 일정을 조회합니다.
     * * 겹치는 조건:
     * (이벤트 시작일 <= 기간 종료일) AND (이벤트 종료일 >= 기간 시작일)
     */
    @Query("SELECT c FROM Calendar c " +
            "WHERE c.memberId = :memberId " +
            "AND c.isDeleted = :isDeleted " +
            "AND c.startDate <= :periodEnd " +
            "AND (c.endDate >= :periodStart OR c.endDate IS NULL)")
    List<Calendar> findOverlappingEvents(
            @Param("memberId") UUID memberId,
            @Param("isDeleted") Bool isDeleted,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    List<Calendar> findByOriginId(UUID originId);

    Optional<Calendar> findByOriginIdAndMemberId(UUID originId, UUID memberId);
}
