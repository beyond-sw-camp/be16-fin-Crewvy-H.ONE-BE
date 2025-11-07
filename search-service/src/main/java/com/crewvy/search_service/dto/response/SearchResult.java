package com.crewvy.search_service.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "category"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EmployeeSearchRes.class, name = "employee"),
    @JsonSubTypes.Type(value = ApprovalSearchRes.class, name = "approval")
})
public interface SearchResult {
    String getId();
    String getCategory();
    String getTitle();
}
