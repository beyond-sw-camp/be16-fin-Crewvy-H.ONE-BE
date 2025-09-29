package com.crewvy.workforce_service.performance.entity;

public enum EvaluationType {
    SELF("본인 평가"),
    SUPERVISOR("상급자 평가");

    private final String description;

    EvaluationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}