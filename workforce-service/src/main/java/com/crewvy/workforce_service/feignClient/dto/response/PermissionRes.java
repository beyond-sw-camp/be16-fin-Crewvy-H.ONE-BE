package com.crewvy.workforce_service.feignClient.dto.response;

import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class PermissionRes {
    private String resource;
    private String action;
    private String description;
    private String permissionRange;
    private Map<String, UUID> rangeToIdMap;
}
