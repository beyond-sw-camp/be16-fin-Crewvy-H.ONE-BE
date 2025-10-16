package com.crewvy.member_service.member.dto.response;

import com.crewvy.common.entity.Bool;
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
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDetailRes {
    private AccountStatus accountStatus;

    private String name;
    private String gradeName;
    private EmploymentType employmentType;
    private MemberStatus memberStatus;
    private String sabun;
    private String email;
    private String phoneNumber;
    private String emergencyContact;
    private String extensionNumber;
    private String telNumber;
    private String address;
    private String bank;
    private String bankAccount;
    private LocalDate joinDate;
    private String lengthOfService;

    private List<GradeHistoryRes> gradeHistoryList;

    private List<TitleRes> titleList;
    private List<OrganizationRes> organizationList;
    private List<MemberPositionRes> memberPositionResList;

    public static MemberDetailRes fromEntity(Member member) {
        List<GradeHistoryRes> gradeHistoryResList = member.getGradeHistorySet().stream()
                .map(GradeHistoryRes::fromEntity).collect(Collectors.toList());

        String gradeName = gradeHistoryResList.stream()
                .filter(gh -> gh.getIsActive() == Bool.TRUE)
                .map(GradeHistoryRes::getGradeName)
                .findFirst()
                .orElse(null);

        List<TitleRes> titleList = member.getMemberPositionList().stream()
                .map(mp -> TitleRes.fromEntity(mp.getTitle())).distinct().collect(Collectors.toList());

        List<OrganizationRes> organizationList = member.getMemberPositionList().stream()
                .map(mp -> OrganizationRes.fromEntity(mp.getOrganization())).distinct().collect(Collectors.toList());

        List<MemberPositionRes> memberPositionResList = member.getMemberPositionList().stream()
                .map(MemberPositionRes::fromEntity).collect(Collectors.toList());

        Period period = Period.between(member.getJoinDate(), LocalDate.now());

        return MemberDetailRes.builder()
                .accountStatus(member.getAccountStatus())
                .name(member.getName())
                .gradeName(gradeName)
                .employmentType(member.getEmploymentType())
                .memberStatus(member.getMemberStatus())
                .sabun(member.getSabun())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .emergencyContact(member.getEmergencyContact())
                .extensionNumber(member.getExtensionNumber())
                .telNumber(member.getTelNumber())
                .address(member.getAddress())
                .bank(member.getBank())
                .bankAccount(member.getBankAccount())
                .joinDate(member.getJoinDate())
                .lengthOfService(String.format("%d년 %d개월 %d일.", period.getYears(), period.getMonths(), period.getDays()))
                .gradeHistoryList(gradeHistoryResList)
                .titleList(titleList)
                .organizationList(organizationList)
                .memberPositionResList(memberPositionResList)
                .build();
    }
}
