package com.crewvy.workforce_service.feignClient.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 연차 발생 계산용 멤버 고용 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEmploymentInfoDto {
    private UUID memberId;
    private UUID companyId;
    private String name;
    private LocalDate joinDate; // 입사일
    private String memberStatus; // WORKING, LEAVE, RETIRED 등
}
