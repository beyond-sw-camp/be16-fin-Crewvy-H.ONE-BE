package com.crewvy.member_service.member.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class OrganizationChangedEvent {
    private final UUID organizationId;

    public OrganizationChangedEvent(UUID organizationId) {
        this.organizationId = organizationId;
    }
}