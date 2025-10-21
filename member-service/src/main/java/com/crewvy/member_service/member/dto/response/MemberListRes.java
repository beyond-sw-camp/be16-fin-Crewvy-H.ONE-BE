package com.crewvy.member_service.member.dto.response;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.MemberPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberListRes {
    private UUID id;
    private String name;
    private String sabun;
    private LocalDate joinDate;
    private String titleName;
    private String organizationName;
    private String memberStatusName;
    private String email;
    private String phoneNumber;
    private String extensionNumber;

    public static MemberListRes fromEntity(MemberPosition memberPosition) {
        return MemberListRes.builder()
                .id(memberPosition.getMember().getId())
                .name(memberPosition.getMember().getName())
                .sabun(memberPosition.getMember().getSabun())
                .joinDate(memberPosition.getMember().getJoinDate())
                .titleName(memberPosition.getTitle().getName())
                .organizationName(memberPosition.getOrganization().getName())
                .memberStatusName(memberPosition.getMember().getMemberStatus().getCodeName())
                .email(memberPosition.getMember().getEmail())
                .phoneNumber(memberPosition.getMember().getIsPhoneNumberPublic() == Bool.TRUE ? memberPosition.getMember().getPhoneNumber() : null)
                .extensionNumber(memberPosition.getMember().getExtensionNumber())
                .build();
    }
}
