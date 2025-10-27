package com.crewvy.member_service.member.dto.response;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberEditRes {
    private MemberDetailRes memberDetail;
    private List<GradeRes> gradeResList;
    private List<OrganizationRes> organizationResList;
    private List<TitleRes> titleResList;
    private List<RoleRes> roleResList;

    public static MemberEditRes toEntity(Member member, List<Grade> allGrade, List<Organization> allOrganization
            , List<Title> allTitle, List<Role> allRole) {
        MemberDetailRes memberDetail = MemberDetailRes.fromEntity(member);

        List<GradeRes> gradeFromEntity = allGrade.stream()
                .map(GradeRes::fromEntity)
                .collect(Collectors.toList());

        List<OrganizationRes> organizationFromEntity = allOrganization.stream()
                .map(OrganizationRes::fromEntity)
                .collect(Collectors.toList());

        List<TitleRes> titleFromEntity = allTitle.stream()
                .map(TitleRes::fromEntity)
                .collect(Collectors.toList());

        List<RoleRes> roleFromEntity = allRole.stream()
                .map(role -> RoleRes.fromEntity(role, Collections.emptyList()))
                .collect(Collectors.toList());

        return MemberEditRes.builder()
                .memberDetail(memberDetail)
                .gradeResList(gradeFromEntity)
                .organizationResList(organizationFromEntity)
                .titleResList(titleFromEntity)
                .roleResList(roleFromEntity)
                .build();
    }
}
