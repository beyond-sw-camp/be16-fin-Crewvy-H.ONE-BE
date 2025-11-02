package com.crewvy.search_service.dto.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ApprovalCompletedEvent {
    private UUID approvalId;
    private UUID memberId;
    private UUID companyId;
    private String title;
    private LocalDateTime createDateTime;
}
