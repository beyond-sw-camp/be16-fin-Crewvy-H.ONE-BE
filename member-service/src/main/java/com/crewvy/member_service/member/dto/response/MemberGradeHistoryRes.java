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
public class MemberGradeHistoryRes {
    private String gradeName;
    private String TitleName;
    private LocalDateTime promotionDate;
}
