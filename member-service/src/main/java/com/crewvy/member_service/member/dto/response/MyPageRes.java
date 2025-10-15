package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Grade;
import com.crewvy.member_service.member.entity.Member;
import com.crewvy.member_service.member.entity.MemberPosition;
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
    private String organizationName;
    private String titleName;
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
    private String employmentTypeName;
    private String defaultPosition;
    private String extensionNumber;     // 내선전화
    private String telNumber;           // 일반전화

    private String bank;
    private String bankAccount;

    public static MyPageRes fromEntity(Member member, MemberPosition memberPosition, Grade grade) {
        Period period = Period.between(member.getJoinDate(), LocalDate.now());
        String defaultPosition = member.getDefaultMemberPosition().getOrganization().getName() + " / "
                + member.getDefaultMemberPosition().getTitle().getName();

        return MyPageRes.builder()
                .profileUrl(member.getProfileUrl())
                .memberName(member.getName())
                .memberStatusName(member.getMemberStatus().getCodeName())
                .organizationName(memberPosition.getOrganization().getName())
                .titleName(memberPosition.getTitle().getName())
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
                .employmentTypeName(member.getEmploymentType().getCodeName())
                .defaultPosition(defaultPosition)
                .extensionNumber(member.getExtensionNumber())
                .telNumber(member.getTelNumber())
                .bank(member.getBank())
                .bankAccount(member.getBankAccount())
                .build();
    }
}
