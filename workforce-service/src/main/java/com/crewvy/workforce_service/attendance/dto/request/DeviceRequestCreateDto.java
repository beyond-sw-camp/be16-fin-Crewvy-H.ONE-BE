package com.crewvy.workforce_service.attendance.dto.request;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
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
public class DeviceRequestCreateDto {

    @NotBlank(message = "디바이스 ID는 필수값입니다.")
    private String deviceId;

    @NotBlank(message = "디바이스 이름은 필수값입니다.")
    private String deviceName;

    @NotNull(message = "디바이스 타입은 필수값입니다.")
    private DeviceType deviceType;

    private String reason;  // 등록 사유 (선택)
}
