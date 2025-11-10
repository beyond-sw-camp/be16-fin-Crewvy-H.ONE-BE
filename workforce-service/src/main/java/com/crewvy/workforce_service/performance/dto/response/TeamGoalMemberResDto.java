package com.crewvy.workforce_service.performance.dto.response;

import com.crewvy.common.entity.Bool;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TeamGoalMemberResDto {
    private UUID memberPositionId;
    private String memberName;
    private String memberTitleName;
    private String memberOrganizationName;
    private Bool isCreater;
}
