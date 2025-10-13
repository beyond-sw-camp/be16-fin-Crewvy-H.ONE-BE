package com.crewvy.member_service.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionRes {
    private UUID id;
    private String name;
    private String description;
    private String resource;
    private String action;
    private String permissionRange;
}
