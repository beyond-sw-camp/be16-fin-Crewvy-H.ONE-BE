package com.crewvy.member_service.member.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class MemberChangedEvent {
    private final UUID memberId;

    public MemberChangedEvent(UUID memberId) {
        this.memberId = memberId;
    }
}
