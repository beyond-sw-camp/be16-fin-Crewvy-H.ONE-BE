package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.constant.AccountStatus;
import com.crewvy.member_service.member.constant.EmploymentType;
import com.crewvy.member_service.member.constant.MemberStatus;
import com.crewvy.member_service.member.entity.GradeHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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


}
