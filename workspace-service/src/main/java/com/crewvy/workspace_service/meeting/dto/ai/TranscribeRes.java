package com.crewvy.workspace_service.meeting.dto.ai;

import com.crewvy.workspace_service.meeting.entity.Recording;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class TranscribeRes {
    private UUID videoConferenceId;
    private String transcript;
    private String summary;
//    private String result;
    private String turnaround;
}
