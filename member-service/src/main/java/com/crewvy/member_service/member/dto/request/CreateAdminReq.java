package com.crewvy.member_service.member.dto.request;

import com.crewvy.member_service.member.constant.EmploymentType;
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
    private String employmentType;
    @NotEmpty(message = "회사명을 입력해 주세요.")
    private String companyName;
    @NotEmpty(message = "사업자 등록번호를 입력해 주세요.")
    private String businessNumber;

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
                .employmentType(EmploymentType.FULL)
                .company(company)
                .build();
    }
}
