package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.workspace_service.meeting.entity.Message;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageRes {
    private UUID id;
    private UUID senderId;
    private String name;
    private String content;
    private LocalDateTime createdAt;

    public static ChatMessageRes fromEntity(Message message, String name) {
        return ChatMessageRes.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .name(name)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
