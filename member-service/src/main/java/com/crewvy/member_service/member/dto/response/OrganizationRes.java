package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Organization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRes {
    private UUID id;
    private String name;
    private UUID parentId;

    public static OrganizationRes fromEntity(Organization organization) {
        return OrganizationRes.builder()
                .id(organization.getId())
                .name(organization.getName())
                .parentId(organization.getParent() != null ? organization.getParent().getId() : null)
                .build();
    }
}
