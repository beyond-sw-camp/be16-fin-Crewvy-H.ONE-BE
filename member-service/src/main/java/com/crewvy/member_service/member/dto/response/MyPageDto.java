package com.crewvy.member_service.member.dto.response;

import com.chillex.gooseBumps.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MyPageDto {
    private Long memberSeq;
    private String email;
    private String name;
    private String nickName;
    private String telNo;
    private String statusMessage;
    private String nationalityCode;
    private String profileImageUrl;
    private String socialType;

    public static MyPageDto fromEntity(Member member){
        return MyPageDto.builder()
                .memberSeq(member.getMemberSeq())
                .email(member.getEmail())
                .name(member.getName())
                .nickName(member.getNickName())
                .telNo(member.getTelNo())
                .statusMessage(member.getStatusMessage())
                .nationalityCode(member.getNationalityCode())
                .profileImageUrl(member.getProfileImageUrl())
                .socialType(member.getSocialType().getCodeName())
                .build();
    }
}
