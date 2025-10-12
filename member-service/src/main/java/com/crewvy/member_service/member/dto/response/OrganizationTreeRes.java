package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Organization;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class OrganizationTreeRes {
    private UUID id;
    private String name;
    private List<OrganizationTreeRes> children;

    public OrganizationTreeRes(Organization organization) {
        this.id = organization.getId();
        this.name = organization.getName();
        this.children = organization.getChildren().stream()
                .sorted(Comparator.comparing(Organization::getDisplayOrder))
                .map(OrganizationTreeRes::new)
                .collect(Collectors.toList());
    }
}
