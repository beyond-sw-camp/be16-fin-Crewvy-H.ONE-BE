package com.crewvy.member_service.member.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class FindPwDto {
    @NotEmpty(message = "이메일을 입력해 주세요.")
    private String email;
    @NotEmpty(message = "휴대폰번호를 입력해 주세요.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "형식에 맞게 입력해 주세요. (예: 010-1234-5678)")
    private String telNo;
    @NotEmpty(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, message = "비밀번호가 너무 짧습니다.")
    private String newPw;
    @NotEmpty(message = "비밀번호 확인을 입력해 주세요.")
    @Size(min = 8, message = "비밀번호가 너무 짧습니다.")
    private String checkNewPw;
}
