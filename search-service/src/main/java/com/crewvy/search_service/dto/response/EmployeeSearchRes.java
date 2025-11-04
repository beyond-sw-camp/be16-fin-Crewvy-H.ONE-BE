package com.crewvy.search_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmployeeSearchRes implements SearchResult {
    private String id;
    private String category = "employee";
    private String title; // Employee name
    private String department;
    private String position;
    private String contact;
    private String email;
    private String status;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
