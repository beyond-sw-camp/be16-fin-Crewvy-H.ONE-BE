package com.crewvy.member_service.member.dto.response;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.Member;
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
public class MemberRes {
    private UUID id;
    private UUID memberPositionId;
    private UUID titleId;
    private String name;
    private String position;
    private String email;
    private String department;
    private String team;
    private String phoneNumber;
    private String status;

    public static MemberRes fromEntity(Member member) {
        MemberPosition defaultPosition = member.getDefaultMemberPosition();
        String departmentName = (defaultPosition != null && defaultPosition.getOrganization() != null) ? defaultPosition.getOrganization().getName() : null;
        String teamName = (defaultPosition != null && defaultPosition.getOrganization() != null && defaultPosition.getOrganization().getParent() != null) ? defaultPosition.getOrganization().getParent().getName() : null;
        String positionName = (defaultPosition != null && defaultPosition.getTitle() != null) ? defaultPosition.getTitle().getName() : null;
        UUID memberPositionId = (defaultPosition != null && defaultPosition.getId() != null) ? defaultPosition.getId() : null;
        UUID titleId = (defaultPosition != null && defaultPosition.getTitle() != null) ? defaultPosition.getTitle().getId() : null;

        return MemberRes.builder()
                .id(member.getId())
                .memberPositionId(memberPositionId)
                .titleId(titleId)
                .name(member.getName())
                .position(positionName)
                .email(member.getEmail())
                .department(departmentName)
                .team(teamName)
                .phoneNumber(member.getIsPhoneNumberPublic() == Bool.TRUE ? member.getPhoneNumber() : null)
                .status(member.getMemberStatus().getCodeName())
                .build();
    }
}
