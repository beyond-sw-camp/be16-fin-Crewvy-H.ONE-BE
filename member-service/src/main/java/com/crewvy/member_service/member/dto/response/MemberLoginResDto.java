package com.crewvy.member_service.member.dto.response;

import com.chillex.gooseBumps.common.constant.code.member.LoginStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MemberLoginResDto {
    private LoginStatus status;
    private String accessToken;
    private String refreshToken;
}
