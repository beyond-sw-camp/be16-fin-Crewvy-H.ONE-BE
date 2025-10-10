package com.crewvy.member_service.member.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class UpdateRolePermissionsReq {

    private List<UUID> permissionIds;
}
