package com.crewvy.member_service.member.dto.request;

import com.crewvy.common.entity.Bool;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradeHistoryReq {
    private UUID gradeHistoryId;
    private UUID gradeId;
    private LocalDate promotionDate;
    private Boolean isActive;

}
