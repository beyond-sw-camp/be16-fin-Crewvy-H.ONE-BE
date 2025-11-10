package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberNameListRes {
    private UUID memberId;
    private String name;

    public static MemberNameListRes fromEntity(Member member){
        return MemberNameListRes.builder()
                .memberId(member.getId())
                .name(member.getName())
                .build();
    }
}
