package com.crewvy.member_service.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRes {
    private String accessToken;
    private String refreshToken;
    private String userName;
    private UUID memberId;
    private UUID memberPositionId;
}