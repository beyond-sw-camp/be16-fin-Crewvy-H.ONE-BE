package com.crewvy.search_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GlobalSearchRes {
    private String id;
    private String category;
    private String title;
    private String snippet;
    private String department;
    private String position;
    private String contact;
    private String status;
}
