package com.crewvy.member_service.member.dto.request;

import com.crewvy.member_service.common.constant.MemberStatus;
import com.crewvy.member_service.common.constant.Bool;
import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Member;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminReq {
    @NotEmpty(message = "이메일을 입력해 주세요.")
    private String email;
    @NotEmpty(message = "비밀번호를 입력해 주세요.")
    private String password;
    @NotEmpty(message = "비밀번호를 입력해 주세요.")
    private String checkPw;
    @NotEmpty(message = "성함을 입력해 주세요.")
    private String name;
    private String telNumber;
    private String address;
    private String bank;
    private String bankAccount;
    private String profileUrl;
    @NotEmpty(message = "회사명을 입력해 주세요.")
    private String companyName;

    public Member toEntity(String encodePassword, Company company){
        return Member.builder()
                .email(this.email)
                .password(encodePassword)
                .name(this.name)
                .telNumber(this.telNumber)
                .address(this.address)
                .bank(this.bank)
                .bankAccount(this.bankAccount)
                .profileUrl(this.profileUrl)
                .company(company)
                .build();
    }
}
