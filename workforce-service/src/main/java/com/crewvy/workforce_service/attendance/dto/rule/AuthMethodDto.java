package com.crewvy.workforce_service.attendance.dto.rule;

import com.crewvy.workforce_service.attendance.constant.DeviceType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class AuthMethodDto {
    private DeviceType deviceType;
    private String authMethod; // GPS, NETWORK_IP
    private Map<String, Object> details;
}
