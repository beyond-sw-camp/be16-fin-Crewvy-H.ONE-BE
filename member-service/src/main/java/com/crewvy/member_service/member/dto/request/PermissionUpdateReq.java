package com.crewvy.member_service.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionUpdateReq {
    private UUID permissionId;
    private String selectedRange;
}
