package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.MemberPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPositionInfo {
    private UUID id;
    private String name;

    public static MemberPositionInfo fromEntity(MemberPosition memberPosition) {
        String positionName = memberPosition.getOrganization().getName() + " / " + memberPosition.getTitle().getName();
        return new MemberPositionInfo(memberPosition.getId(), positionName);
    }
}
