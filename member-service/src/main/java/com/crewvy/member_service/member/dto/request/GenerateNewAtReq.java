package com.crewvy.member_service.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class GenerateNewAtReq {
    private String refreshToken;
    private UUID memberPositionId;
}
