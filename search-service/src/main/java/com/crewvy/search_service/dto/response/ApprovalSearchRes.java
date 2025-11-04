package com.crewvy.search_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApprovalSearchRes implements SearchResult {
    private String id;
    private String category = "approval";
    private String title; // Approval title
    private String titleName;
    private String memberName;
    private String createAt;

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
