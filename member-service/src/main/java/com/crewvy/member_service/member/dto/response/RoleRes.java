package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRes {
    private UUID id;
    private String name;
    private String description;
    private int memberCount;
    private List<String> permissionList;
    private List<RoleMemberRes> memberList;

    public static RoleRes fromEntity(Role role, List<RoleMemberRes> memberList) {
        return RoleRes.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .memberCount(memberList.size())
                .permissionList(role.getRolePermissionList().stream()
                        .map(rp -> rp.getPermission().getName())
                        .collect(Collectors.toList()))
                .memberList(memberList)
                .build();
    }

    public static RoleRes forMemberPositionRes(Role role){
        return RoleRes.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissionList(role.getRolePermissionList().stream()
                        .map(rp -> rp.getPermission().getName())
                        .collect(Collectors.toList()))
                .build();
    }
}
