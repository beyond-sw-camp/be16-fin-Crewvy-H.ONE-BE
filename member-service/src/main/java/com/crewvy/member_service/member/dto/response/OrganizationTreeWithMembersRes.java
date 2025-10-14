package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Organization;
import com.crewvy.member_service.member.entity.MemberPosition;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class OrganizationTreeWithMembersRes {
    private UUID id;
    private String label;
    private String type;
    private List<MemberRes> members;
    private List<OrganizationTreeWithMembersRes> children;

    public OrganizationTreeWithMembersRes(Organization organization, List<MemberPosition> allMemberPositions) {
        this.id = organization.getId();
        this.label = organization.getName();
        this.type = determineOrganizationType(organization);
        this.members = allMemberPositions.stream()
                .filter(mp -> mp.getOrganization().getId().equals(organization.getId()))
                .map(MemberPosition::getMember)
                .distinct()
                .map(MemberRes::fromEntity)
                .collect(Collectors.toList());
        this.children = organization.getChildren().stream()
                .sorted(Comparator.comparing(Organization::getDisplayOrder))
                .map(child -> new OrganizationTreeWithMembersRes(child, allMemberPositions))
                .collect(Collectors.toList());
    }

    private String determineOrganizationType(Organization organization) {
        if (organization.getParent() == null) {
            return "company";
        } else if (organization.getChildren().isEmpty()) {
            return "team";
        } else {
            return "department";
        }
    }
}
