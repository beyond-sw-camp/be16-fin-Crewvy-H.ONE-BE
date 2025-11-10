package com.crewvy.member_service.member.dto.request;

import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Title;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTitleReq {
    private String name;

    public Title toEntity(Company company){
        return Title.builder()
                .name(this.name)
                .company(company)
                .build();
    }
}
