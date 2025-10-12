package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.MemberPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleMemberRes {
    private String name;
    private String organizationName;
    private String position;

    public static RoleMemberRes fromEntity(MemberPosition memberPosition) {
        return RoleMemberRes.builder()
                .name(memberPosition.getMember().getName())
                .organizationName(memberPosition.getOrganization().getName())
                .position(memberPosition.getTitle().getName())
                .build();
    }
}
