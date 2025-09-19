package com.crewvy.member_service.member.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) //없는 필드는 자동무시

public class GoogleProfileDto {
    private String sub;
    private String email;
    private String picture;
    private String name;
}
