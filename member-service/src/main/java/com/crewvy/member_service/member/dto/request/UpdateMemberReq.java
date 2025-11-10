package com.crewvy.member_service.member.dto.request;

import com.crewvy.member_service.member.dto.response.MemberPositionRes;
import com.crewvy.member_service.member.dto.response.OrganizationRes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberReq {
    private String accountStatusCodeValue;
    private String newPw;

    private String name;
    private String employmentTypeCodeValue;
    private String memberStatusCodeValue;
    private String sabun;
    private String extensionNumber;
    private String telNumber;
    private String address;
    private String detailAddress;
    private LocalDate joinDate;
    private List<GradeHistoryReq> gradeHistoryReqList;
    private List<PositionUpdateReq> positionUpdateReqList;
    private List<OrganizationRes> organizationList;
    private List<MemberPositionRes> memberPositionResList;
}
