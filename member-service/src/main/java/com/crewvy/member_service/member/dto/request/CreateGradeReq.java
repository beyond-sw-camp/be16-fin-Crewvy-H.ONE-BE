package com.crewvy.member_service.member.dto.request;

import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Grade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGradeReq {
    private String name;

    public Grade toEntity(Company company){
        return Grade.builder()
                .name(this.name)
                .company(company)
                .build();
    }
}
