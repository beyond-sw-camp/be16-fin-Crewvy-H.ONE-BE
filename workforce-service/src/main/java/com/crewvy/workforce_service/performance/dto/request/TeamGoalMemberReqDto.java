package com.crewvy.workforce_service.performance.dto.request;

import com.crewvy.common.entity.Bool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TeamGoalMemberReqDto {
    private UUID memberPositionId;
    private Bool isCreater;
}
