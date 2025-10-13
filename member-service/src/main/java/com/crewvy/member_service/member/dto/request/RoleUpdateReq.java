package com.crewvy.member_service.member.dto.request;

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
public class RoleUpdateReq {
    private String name;
    private String description;
    private List<PermissionUpdateReq> permissions;
}
