package com.crewvy.workforce_service.feignClient.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrganizationNodeDto {
    private UUID id;
    private String label;
    private String type;
    @Builder.Default
    List<MemberDto> members = new ArrayList<>();
    @Builder.Default
    @JsonProperty("children")
    List<OrganizationNodeDto> children = new ArrayList<>();
}
