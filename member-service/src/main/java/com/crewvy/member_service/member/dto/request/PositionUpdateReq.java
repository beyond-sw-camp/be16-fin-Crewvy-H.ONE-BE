package com.crewvy.member_service.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; // LocalDateTime import 추가
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PositionUpdateReq {
    private UUID memberPositionId;
    private UUID organizationId;
    private UUID titleId;
    private UUID roleId;
    private LocalDateTime startDate;
}
