package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.constant.AccountStatus;
import com.crewvy.member_service.member.constant.EmploymentType;
import com.crewvy.member_service.member.constant.MemberStatus;
import com.crewvy.member_service.member.entity.GradeHistory;
import com.crewvy.member_service.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDetailRes {
    private String name;
    private List<TitleRes> titleList;
    private List<OrganizationRes> organizationList;
    private MemberStatus memberStatus;
    private AccountStatus accountStatus;
    private EmploymentType employmentType;
    private String email;
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
    private List<MemberPositionRes> memberPositionResList;
    private Set<GradeHistory> gradeHistorySet;

    public static MemberDetailRes fromEntity(Member member) {
        List<TitleRes> titleList = member.getMemberPositionList().stream()
                .map(mp -> TitleRes.fromEntity(mp.getTitle())).distinct().collect(Collectors.toList());

        List<OrganizationRes> organizationList = member.getMemberPositionList().stream()
                .map(mp -> OrganizationRes.fromEntity(mp.getOrganization())).distinct().collect(Collectors.toList());

        List<MemberPositionRes> memberPositionResList = member.getMemberPositionList().stream()
                .map(MemberPositionRes::fromEntity).collect(Collectors.toList());

        return MemberDetailRes.builder()
                .name(member.getName())
                .titleList(titleList)
                .organizationList(organizationList)
                .memberStatus(member.getMemberStatus())
                .accountStatus(member.getAccountStatus())
                .employmentType(member.getEmploymentType())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .emergencyContact(member.getEmergencyContact())
                .address(member.getAddress())
                .bank(member.getBank())
                .bankAccount(member.getBankAccount())
                .profileUrl(member.getProfileUrl())
                .sabun(member.getSabun())
                .joinDate(member.getJoinDate())
                .extensionNumber(member.getExtensionNumber())
                .telNumber(member.getTelNumber())
                .memberPositionResList(memberPositionResList)
                .gradeHistorySet(member.getGradeHistorySet())
                .build();
    }
}
