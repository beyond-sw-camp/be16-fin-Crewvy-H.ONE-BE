package com.crewvy.workforce_service.approval.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ApprovalCompletedEvent {
    private final UUID approvalId;
    private final UUID memberId;
    private final UUID companyId;
    private final String title;
    private final String memberName;
    private final String titleName;
    private final LocalDateTime createDateTime;
}
