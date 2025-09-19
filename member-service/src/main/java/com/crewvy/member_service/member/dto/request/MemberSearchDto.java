package com.crewvy.member_service.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter

public class MemberSearchDto {
    private String email;
    private String name;
}
