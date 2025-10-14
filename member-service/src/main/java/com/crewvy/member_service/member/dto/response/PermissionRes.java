package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.constant.PermissionRange;
import com.crewvy.member_service.member.entity.Permission;
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
    private PermissionRange permissionRange;
    private Map<PermissionRange, UUID> rangeToIdMap;

    public static PermissionRes fromEntity(Permission permission, PermissionRange permissionRange,
                                           Map<PermissionRange, UUID> rangeToIdMap){
        return PermissionRes.builder()
                .resource(permission.getResource())
                .action(permission.getAction().getCodeName())
                .description(permission.getDescription())
                .permissionRange(permissionRange)
                .rangeToIdMap(rangeToIdMap)
                .build();
    }
}
