package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.constant.AccountStatus;
import com.crewvy.member_service.member.constant.EmploymentType;
import com.crewvy.member_service.member.constant.MemberStatus;
import com.crewvy.member_service.member.entity.GradeHistory;
import com.crewvy.member_service.member.entity.MemberPosition;
import com.crewvy.member_service.member.entity.Organization;
import com.crewvy.member_service.member.entity.Title;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDetailRes {
    private String name;
    private List<Title> titleList;
    private List<Organization> organizationList;
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
    private String extensionNumber;
    private String telNumber;
    private List<MemberPositionRes> memberPositionResList;
    private List<GradeHistory> gradeHistoryList;
}
