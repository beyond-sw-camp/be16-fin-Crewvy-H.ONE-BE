package com.crewvy.workforce_service.attendance.repository;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {
    @Query("SELECT count(r) > 0 FROM Request r WHERE r.memberId = :memberId AND r.deviceId = :deviceId AND r.deviceType = :deviceType AND r.status = :status")
    boolean existsApprovedDevice(@Param("memberId") UUID memberId,
                                 @Param("deviceId") String deviceId,
                                 @Param("deviceType") DeviceType deviceType,
                                 @Param("status") RequestStatus status);
}
