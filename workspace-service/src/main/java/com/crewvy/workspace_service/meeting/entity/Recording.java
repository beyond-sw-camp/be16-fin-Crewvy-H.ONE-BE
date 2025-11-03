package com.crewvy.workspace_service.meeting.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import livekit.LivekitEgress.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recording extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "video_conference_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private VideoConference videoConference;

    @Column(name = "filename", nullable = false, columnDefinition = "TEXT")
    private String filename;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "bytes", nullable = false)
    private Long bytes;

    @Column(name = "duration", nullable = false)
    private Long duration;

    @OneToOne(mappedBy = "recording", cascade = CascadeType.ALL, orphanRemoval = true)
    private Minute minute;

    public static Recording fromFileInfo(FileInfo fileInfo, VideoConference videoConference) {
        return Recording.builder()
                .filename(fileInfo.getFilename())
                .url(fileInfo.getLocation())
                .bytes(fileInfo.getSize())
                .duration(fileInfo.getDuration())
                .videoConference(videoConference)
                .build();
    }
}
