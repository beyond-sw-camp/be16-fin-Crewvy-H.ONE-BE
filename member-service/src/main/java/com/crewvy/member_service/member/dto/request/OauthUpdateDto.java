package com.crewvy.member_service.member.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder

public class OauthUpdateDto {
    @NotEmpty(message = "성함을 입력해 주세요.")
    private String name;
    @NotEmpty(message = "닉네임을 입력해 주세요.")
    private String nickName;
    @NotEmpty(message = "휴대폰번호를 입력해 주세요.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "형식에 맞게 입력해 주세요. (예: 010-1234-5678)")
    private String telNo;
    @NotEmpty(message = "국적을 입력해 주세요.")
    private String nationalityCode;
    @NotNull(message = "생년월일을 입력해 주세요.")
    private LocalDate birthDate;
}
