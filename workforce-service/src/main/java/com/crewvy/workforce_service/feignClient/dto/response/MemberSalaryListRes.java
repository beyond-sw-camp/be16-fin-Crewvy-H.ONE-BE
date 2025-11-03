package com.crewvy.workforce_service.feignClient.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSalaryListRes {
    private UUID memberId;
    private String sabun;
    private String memberName;
    private String organizationName;
    private String titleName;
    private String bank;
    private String bankAccount;
}
