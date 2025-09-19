package com.crewvy.member_service.member.dto.request;

import com.chillex.gooseBumps.common.constant.code.member.SocialType;
import com.chillex.gooseBumps.domain.member.entity.Member;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder

public class MemberCreateDto {
    @NotEmpty(message = "이메일을 입력해 주세요.")
    private String email;
    @NotEmpty(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, message = "비밀번호가 너무 짧습니다.")
    private String password;
    @NotEmpty(message = "비밀번호 확인을 입력해 주세요.")
    @Size(min = 8, message = "비밀번호가 너무 짧습니다.")
    private String checkPw;
    @NotEmpty(message = "이름을 입력해 주세요.")
    private String name;
    @NotEmpty(message = "닉네임 입력해 주세요.")
    private String nickName;
    @NotEmpty(message = "휴대폰번호를 입력해 주세요.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "형식에 맞게 입력해 주세요. (예: 010-1234-5678)")
    private String telNo;
    @NotNull(message = "생년월일을 입력해 주세요.")
    private LocalDate birthDate;
    @NotEmpty(message = "국적을 입력해 주세요.")
    private String nationalityCode;
    private MultipartFile profileImage;
    @Builder.Default
    private String socialType = SocialType.GOOSEBUMPS.getCodeValue();
    @Builder.Default
    private String socialId = "gooseBumps";

    public Member toEntity(String encodePassword) {
        return Member.builder()
                .email(this.email)
                .password(encodePassword)
                .name(this.name)
                .nickName(this.nickName)
                .telNo(this.telNo)
                .birthDate(this.birthDate)
                .nationalityCode(this.nationalityCode)
                .socialType(SocialType.fromCode(this.socialType))
                .socialId(this.socialId)
                .build();
    }
}
