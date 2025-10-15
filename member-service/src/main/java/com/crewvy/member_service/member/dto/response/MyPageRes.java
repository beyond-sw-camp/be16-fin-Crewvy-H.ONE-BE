package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Grade;
import com.crewvy.member_service.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPageRes {
    private String profileUrl;
    private String memberName;
    private String memberStatusName;
    private String defaultOrganizationName;
    private String defaultTitleName;
    private String email;
    private String phoneNumber;
    private boolean isPhoneNumberPublic;
    private String emergencyContact;
    private String address;
    private boolean isAddressDisclosure;

    private String gradeName;
    private String sabun;
    private LocalDate joinDate;
    private String lengthOfService;
    private String extensionNumber;     // 내선전화
    private String telNumber;           // 일반전화

    private String bank;
    private String bankAccount;

    public static MyPageRes fromEntity(Member member, Grade grade) {
        Period period = Period.between(member.getJoinDate(), LocalDate.now());

        return MyPageRes.builder()
                .profileUrl(member.getProfileUrl())
                .memberName(member.getName())
                .memberStatusName(member.getMemberStatus().getCodeName())
                .defaultOrganizationName(member.getDefaultMemberPosition().getOrganization().getName())
                .defaultTitleName(member.getDefaultMemberPosition().getTitle().getName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .isPhoneNumberPublic(member.getIsPhoneNumberPublic().toBoolean())
                .emergencyContact(member.getEmergencyContact())
                .address(member.getAddress())
                .isAddressDisclosure(member.getIsAddressDisclosure().toBoolean())
                .gradeName(grade.getName())
                .sabun(member.getSabun())
                .joinDate(member.getJoinDate())
                .lengthOfService(String.format("%d년 %d개월 %d일.", period.getYears(), period.getMonths(), period.getDays()))
                .extensionNumber(member.getExtensionNumber())
                .telNumber(member.getTelNumber())
                .bank(member.getBank())
                .bankAccount(member.getBankAccount())
                .build();
    }
}
