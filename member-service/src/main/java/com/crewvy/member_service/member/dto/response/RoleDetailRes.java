package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDetailRes {
    private UUID id;
    private String name;
    private String description;
    private List<PermissionRes> permissionResList;

    public static RoleDetailRes fromEntity(Role role, List<PermissionRes> permissionResList){
        return RoleDetailRes.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissionResList(permissionResList)
                .build();
    }
}
