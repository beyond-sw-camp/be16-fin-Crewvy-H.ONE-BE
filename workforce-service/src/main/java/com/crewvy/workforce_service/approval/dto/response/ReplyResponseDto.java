package com.crewvy.workforce_service.approval.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ReplyResponseDto {
    private String contents;
    private UUID memberPositionId;
    private String memberName;
    private String memberPosition;
    private String memberOrganization;
    private LocalDateTime createdAt;
}
