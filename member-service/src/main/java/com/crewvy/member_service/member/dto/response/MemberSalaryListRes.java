package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.MemberPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSalaryListRes {
    private UUID memberId;
    private String sabun;
    private String memberName;
    private String organizationName;
    private String titleName;
    private String bank;
    private String bankAccount;

    public static MemberSalaryListRes fromEntity(MemberPosition memberPosition){
        return MemberSalaryListRes.builder()
                .memberId(memberPosition.getMember().getId())
                .sabun(memberPosition.getMember().getSabun())
                .memberName(memberPosition.getMember().getName())
                .organizationName(memberPosition.getOrganization().getName())
                .titleName(memberPosition.getTitle().getName())
                .bank(memberPosition.getMember().getBank())
                .bankAccount(memberPosition.getMember().getBankAccount())
                .build();
    }
}
