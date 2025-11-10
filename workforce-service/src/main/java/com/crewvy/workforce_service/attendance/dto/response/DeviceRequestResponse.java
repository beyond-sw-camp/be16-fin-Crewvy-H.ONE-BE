package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.entity.Request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequestResponse {

    private UUID requestId;
    private String deviceId;
    private String deviceName;
    private DeviceType deviceType;
    private RequestStatus status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static DeviceRequestResponse from(Request request) {
        return DeviceRequestResponse.builder()
                .requestId(request.getId())
                .deviceId(request.getDeviceId())
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .status(request.getStatus())
                .reason(request.getReason())
                .createdAt(request.getCreatedAt())
                .completedAt(request.getCompletedAt())
                .build();
    }
}
