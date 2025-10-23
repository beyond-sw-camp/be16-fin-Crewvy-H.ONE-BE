package com.crewvy.workforce_service.feignClient.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRes {
    private UUID id;
    private String name;
    private UUID parentId;
}
