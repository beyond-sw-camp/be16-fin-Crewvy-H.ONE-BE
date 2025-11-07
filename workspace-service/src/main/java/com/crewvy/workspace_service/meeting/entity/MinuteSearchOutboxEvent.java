package com.crewvy.workspace_service.meeting.entity;

import com.crewvy.common.entity.Bool;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class MinuteSearchOutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String topic;

    private UUID aggregateId;

    @Lob
    private String payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Bool processed = Bool.FALSE;

    public void setProcessed() {
        this.processed = Bool.TRUE;
    }
}
