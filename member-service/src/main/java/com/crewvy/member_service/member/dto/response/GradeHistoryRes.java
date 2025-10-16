package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.GradeHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.crewvy.common.entity.Bool;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeHistoryRes {
    private UUID gradeHistoryId;
    private UUID gradeId;
    private String gradeName;
    private LocalDate promotionDate;
    private Bool isActive;

    public static GradeHistoryRes fromEntity(GradeHistory gradeHistory) {
        return GradeHistoryRes.builder()
                .gradeHistoryId(gradeHistory.getId())
                .gradeId(gradeHistory.getGrade().getId())
                .gradeName(gradeHistory.getGrade().getName())
                .promotionDate(gradeHistory.getPromotionDate())
                .isActive(gradeHistory.getIsActive())
                .build();
    }
}
