package com.crewvy.workforce_service.attendance.dto.request;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
import com.crewvy.workforce_service.attendance.constant.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EventRequest {
    @NotNull(message = "이벤트 타입은 필수값입니다.")
    private EventType eventType;

    // deviceId는 근태 인증에 사용하지 않음 (로그인으로 사용자 판별, deviceType으로 인증 방식 구분)
    // @NotBlank(message = "디바이스 ID는 필수값입니다.")
    // private String deviceId;

    @NotNull(message = "디바이스 타입은 필수값입니다.")
    private DeviceType deviceType;

    private Double latitude;
    private Double longitude;

    private String wifiSsid;        // WiFi SSID (네트워크 이름)
    private String wifiBssid;       // WiFi BSSID (MAC 주소, 선택 사항)
}
