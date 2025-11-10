package com.crewvy.member_service.member.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginReq {
    @NotEmpty(message = "이메일을 입력해 주세요.")
    private String email;
    @NotEmpty(message = "비밀번호를 입력해 주세요.")
    private String password;
}
