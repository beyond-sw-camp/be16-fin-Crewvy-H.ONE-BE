package com.crewvy.workforce_service.approval.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ApprovalCompletedEvent {
    private final UUID approvalId;
    private final UUID memberId;
    private final String title;
    private final String titleName;
    private final String memberName;
    private final List<String> approvalLineList;
    private final LocalDateTime createDateTime;
}
