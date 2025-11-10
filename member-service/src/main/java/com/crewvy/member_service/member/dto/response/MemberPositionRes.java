package com.crewvy.member_service.member.dto.response;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.MemberPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;
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
    private LocalDate startDate;
    private LocalDate endDate;
    private String lengthOfService;
    private Bool isActive;
    private Bool ynDel;

    public static MemberPositionRes fromEntity(MemberPosition memberPosition) {
        String lengthOfService = null;
        if (memberPosition.getStartDate() != null) {
            LocalDate endDate = memberPosition.getEndDate() != null ? memberPosition.getEndDate() : LocalDate.now();
            Period period = Period.between(memberPosition.getStartDate(), endDate);
            lengthOfService = String.format("%d년 %d개월 %d일", period.getYears(), period.getMonths(), period.getDays());
        }
        return MemberPositionRes.builder()
                .id(memberPosition.getId())
                .organization(OrganizationRes.fromEntity(memberPosition.getOrganization()))
                .title(TitleRes.fromEntity(memberPosition.getTitle()))
                .role(RoleRes.forMemberPositionRes(memberPosition.getRole()))
                .startDate(memberPosition.getStartDate())
                .endDate(memberPosition.getEndDate())
                .lengthOfService(lengthOfService)
                .isActive(memberPosition.getIsActive())
                .ynDel(memberPosition.getYnDel())
                .build();
    }
}