package com.crewvy.workforce_service.feignClient.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PositionDto {
    private UUID memberId;
    private UUID memberPositionId;
    private String memberName;
    private String organizationName;
    private String titleName;
}
