package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.entity.WorkLocation;
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
public class WorkLocationResponse {

    private UUID workLocationId;

    private UUID companyId;

    private String name;

    private String address;

    private Double latitude;

    private Double longitude;

    private Integer gpsRadius;

    private String ipAddress;

    private String wifiSsid;

    private String wifiBssid;

    private Boolean isActive;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static WorkLocationResponse from(WorkLocation workLocation) {
        return WorkLocationResponse.builder()
                .workLocationId(workLocation.getId())
                .companyId(workLocation.getCompanyId())
                .name(workLocation.getName())
                .address(workLocation.getAddress())
                .latitude(workLocation.getLatitude())
                .longitude(workLocation.getLongitude())
                .gpsRadius(workLocation.getGpsRadius())
                .ipAddress(workLocation.getIpAddress())
                .wifiSsid(workLocation.getWifiSsid())
                .wifiBssid(workLocation.getWifiBssid())
                .isActive(workLocation.getIsActive())
                .description(workLocation.getDescription())
                .createdAt(workLocation.getCreatedAt())
                .updatedAt(workLocation.getUpdatedAt())
                .build();
    }
}
