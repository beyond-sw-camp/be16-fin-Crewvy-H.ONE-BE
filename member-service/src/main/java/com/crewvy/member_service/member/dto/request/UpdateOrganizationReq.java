package com.crewvy.member_service.member.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationReq {
    @NotEmpty(message = "조직명을 입력해 주세요.")
    private String name;
}
