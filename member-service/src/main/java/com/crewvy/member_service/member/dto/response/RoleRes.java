package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRes {
    private UUID id;
    private String name;

    public static RoleRes fromEntity(Role role) {
        return RoleRes.builder()
                .id(role.getId())
                .name(role.getName())
                .build();
    }
}
