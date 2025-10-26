package com.crewvy.workforce_service.attendance.dto.rule;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * 인증 규칙 DTO
 * WorkLocation 참조 방식으로 변경 (출장지, 지점 등 다양한 근무지 지원)
 */
@Getter
@Setter
public class AuthRuleDto {
    /**
     * 허용된 근무지 ID 목록
     * WorkLocation 테이블 참조
     */
    private List<UUID> allowedWorkLocationIds;

    /**
     * 필수 인증 방식 목록
     * 가능한 값: "GPS", "WIFI", "IP"
     * 예: ["GPS", "WIFI"] - GPS와 WiFi 둘 다 필요
     */
    private List<String> requiredAuthTypes;
}
