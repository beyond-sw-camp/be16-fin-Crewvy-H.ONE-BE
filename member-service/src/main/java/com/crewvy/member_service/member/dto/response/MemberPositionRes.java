package com.crewvy.member_service.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPositionRes {
    private String organizationName;
    private String TitleName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
