package com.crewvy.workforce_service.approval.entity;

public enum ApprovalState {
    DRAFT("임시저장"),
    PENDING("진행중"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String description;

    ApprovalState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}