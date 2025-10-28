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
public class MemberPositionListRes {
    private UUID memberId;
    private UUID memberPositionId;
    private String organizationName;
    private String titleName;
    private String memberName;
}
