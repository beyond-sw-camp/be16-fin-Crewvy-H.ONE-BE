package com.crewvy.member_service.member.dto.request;

import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Organization;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationReq {
    private UUID parentId;
    private String name;

    public Organization toEntity(Organization parent, Company company){
        return Organization.builder()
                .parent(parent)
                .name(this.name)
                .company(company)
                .build();
    }
}
