package com.crewvy.member_service.member.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class UpdateMemberRoleReq {

    @NotNull(message = "새로운 역할 ID는 필수입니다.")
    private UUID newRoleId;
}
