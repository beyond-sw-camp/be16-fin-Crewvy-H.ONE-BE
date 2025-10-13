package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.constant.PermissionRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionRes {
    private String resource;
    private String action;
    private String description;
    private PermissionRange currentRange;
    private Map<PermissionRange, UUID> rangeToIdMap;
}
