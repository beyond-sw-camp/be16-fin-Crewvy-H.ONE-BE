package com.crewvy.workforce_service.feignClient.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MemberDto {
    private UUID id;
    private UUID memberPositionId;
    private UUID titleId;
    private String name;
    private String position;
    private String email;
    private String department;
    private String team;
}
