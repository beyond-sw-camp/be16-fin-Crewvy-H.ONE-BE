package com.crewvy.member_service.member.dto.request;

import com.crewvy.member_service.member.constant.EmploymentType;
import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Member;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder    // AutoCreate 에서 사용
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
    private LocalDate joinDate;
    private String extensionNumber;
    private String telNumber;
    @NotNull(message = "고용형태를 선택해 주세요.")
    private String employmentType;
    @NotNull(message = "조직을 선택해 주세요.")
    private UUID organizationId;
    @NotNull(message = "직책을 선택해 주세요.")
    private UUID titleId;
    @NotNull(message = "직급을 선택해 주세요.")
    private UUID gradeId;
    @NotNull(message = "역할을 선택해 주세요.")
    private UUID roleId;

    public Member memberToEntity(String encodePassword, Company company){
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
                .joinDate(this.joinDate)
                .extensionNumber(this.extensionNumber)
                .telNumber(this.telNumber)
                .employmentType(EmploymentType.fromCode(this.employmentType))
                .company(company)
                .build();
    }
}
