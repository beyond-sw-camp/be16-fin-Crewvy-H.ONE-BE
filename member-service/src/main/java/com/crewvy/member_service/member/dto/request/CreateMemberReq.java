package com.crewvy.member_service.member.dto.request;

import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Member;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberReq {
    @NotEmpty(message = "이메일을 입력해 주세요.")
    private String email;
    @NotEmpty(message = "비밀번호를 입력해 주세요.")
    private String password;
    @NotEmpty(message = "성함을 입력해 주세요.")
    private String name;
    private LocalDate localDate;
    private String phoneNumber;
    private String emergencyContact;
    private String address;
    private String bank;
    private String bankAccount;
    private String profileUrl;
    private String sabun;
    private String extensionNumber;
    private String telNumber;
    private UUID organizationId;

    public Member toEntity(String encodePassword, Company company){
        return Member.builder()
                .email(this.email)
                .password(encodePassword)
                .name(this.name)
                .phoneNumber(this.phoneNumber)
                .address(this.address)
                .bank(this.bank)
                .bankAccount(this.bankAccount)
                .profileUrl(this.profileUrl)
                .sabun(this.sabun)
                .extensionNumber(this.extensionNumber)
                .telNumber(this.telNumber)
                .company(company)
                .build();
    }
}
