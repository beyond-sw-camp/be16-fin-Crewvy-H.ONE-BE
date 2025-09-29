package com.crewvy.workspace_service.meeting.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.converter.MinuteStatusConverter;
import com.crewvy.workspace_service.meeting.converter.VideoConferenceStatusConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoConference extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "password", nullable = true)
    private String password;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "scheduled_start_time", nullable = true)
    private LocalDateTime scheduledStartTime;

    @Column(name = "actual_start_time", nullable = true)
    private LocalDateTime actualStartTime;

    @Column(name = "end_time", nullable = true)
    private LocalDateTime endTime;

    @Builder.Default
    @Column(name = "status", nullable = false)
    @Convert(converter = VideoConferenceStatusConverter.class)
    private VideoConferenceStatus status = VideoConferenceStatus.WAITING;

    @Builder.Default
    @OneToMany(mappedBy = "videoConference", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messageList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "videoConference", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoConferenceParticipant> videoConferenceParticipantList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "videoConference", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageMedia> messageMediaList = new ArrayList<>();
}
