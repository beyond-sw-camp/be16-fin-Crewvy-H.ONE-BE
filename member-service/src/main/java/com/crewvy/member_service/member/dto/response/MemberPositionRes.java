package com.crewvy.member_service.member.dto.response;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.MemberPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPositionRes {
    private UUID id;
    private OrganizationRes organization;
    private TitleRes title;
    private RoleRes role;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Bool isActive;

    public static MemberPositionRes fromEntity(MemberPosition memberPosition) {
        return MemberPositionRes.builder()
                .id(memberPosition.getId())
                .organization(OrganizationRes.fromEntity(memberPosition.getOrganization()))
                .title(TitleRes.fromEntity(memberPosition.getTitle()))
                .role(RoleRes.fromEntity(memberPosition.getRole()))
                .startDate(memberPosition.getStartDate())
                .endDate(memberPosition.getEndDate())
                .isActive(memberPosition.getIsActive())
                .build();
    }
}