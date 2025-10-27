package com.crewvy.member_service.member.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class OrganizationChangedEvent {
    private final UUID organizationId;
}