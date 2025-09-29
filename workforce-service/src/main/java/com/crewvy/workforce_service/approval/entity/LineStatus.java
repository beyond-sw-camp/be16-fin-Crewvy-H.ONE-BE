package com.crewvy.workforce_service.approval.entity;

public enum LineStatus {
    WAITING("대기"),    // 아직 결재 순서가 아님
    PENDING("진행중"),  // 현재 결재할 차례
    APPROVED("승인"),   // 해당 결재자가 승인함
    REJECTED("반려");   // 해당 결재자가 반려함

    private final String description;

    LineStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}