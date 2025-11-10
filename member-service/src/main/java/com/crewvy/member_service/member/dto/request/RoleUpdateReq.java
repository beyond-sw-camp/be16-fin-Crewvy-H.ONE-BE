package com.crewvy.member_service.member.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpdateReq {
    @NotEmpty(message = "이름을 입력해 주세요.")
    private String name;
    private String description;
    private List<PermissionUpdateReq> permissions;
}
