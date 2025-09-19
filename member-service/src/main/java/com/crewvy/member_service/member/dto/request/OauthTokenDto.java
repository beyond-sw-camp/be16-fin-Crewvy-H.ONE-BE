package com.crewvy.member_service.member.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@JsonIgnoreProperties(ignoreUnknown = true) //없는 필드는 자동무시

public class OauthTokenDto {
    private String access_token;
    private String expires_in;
    private String scope;
    private String id_token;
}
