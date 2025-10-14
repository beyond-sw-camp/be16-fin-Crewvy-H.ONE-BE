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
public class MemberPositionListRes {
    private UUID memberId;
    private UUID memberPositionId;
    private String memberName;
    private String organizationName;
    private String titleName;

    public static MemberPositionListRes fromEntity(MemberPosition memberPosition){
        return MemberPositionListRes.builder()
                .memberId(memberPosition.getMember().getId())
                .memberPositionId(memberPosition.getId())
                .memberName(memberPosition.getMember().getName())
                .organizationName(memberPosition.getOrganization().getName())
                .titleName(memberPosition.getTitle().getName())
                .build();
    }
}
