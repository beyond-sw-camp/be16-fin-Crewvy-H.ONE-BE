package com.crewvy.workspace_service.meeting.dto.ai;

import com.crewvy.workspace_service.meeting.entity.Recording;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class TranscribeReq {
    private UUID videoConferenceId;
    private String filename;
    private String url;
    private Long bytes;
    private Long duration;

    public static TranscribeReq fromEntity(Recording recording) {
        return TranscribeReq.builder()
                .videoConferenceId(recording.getVideoConference().getId())
                .filename(recording.getFilename())
                .url(recording.getUrl())
                .bytes(recording.getBytes())
                .duration(recording.getDuration())
                .build();
    }
}
