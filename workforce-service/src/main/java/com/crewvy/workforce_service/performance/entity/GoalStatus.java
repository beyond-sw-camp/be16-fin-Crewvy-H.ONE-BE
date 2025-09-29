package com.crewvy.workforce_service.performance.entity;

public enum GoalStatus {
    REQUESTED("요청"),
    APPROVED("승인"),
    REJECTED("반려"),
    COMPLETED("완료"),
    CANCELED("취소");

    private final String description;

    GoalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}