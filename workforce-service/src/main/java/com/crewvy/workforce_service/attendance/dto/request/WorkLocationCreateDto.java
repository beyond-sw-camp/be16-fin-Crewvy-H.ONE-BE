package com.crewvy.workforce_service.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLocationCreateDto {

    @NotBlank(message = "근무지 명칭은 필수입니다.")
    private String name;                // 근무지 명칭

    private String address;             // 주소

    private Double latitude;            // 위도 (GPS)

    private Double longitude;           // 경도 (GPS)

    private Integer gpsRadius;          // GPS 허용 반경 (미터)

    private String ipAddress;           // IP 주소 또는 IP 대역

    private String wifiSsid;            // WiFi SSID (네트워크 이름)

    private String wifiBssid;           // WiFi BSSID (MAC 주소, 선택 사항)

    @NotNull(message = "활성 상태는 필수입니다.")
    private Boolean isActive;           // 활성/비활성

    private String description;         // 설명
}
