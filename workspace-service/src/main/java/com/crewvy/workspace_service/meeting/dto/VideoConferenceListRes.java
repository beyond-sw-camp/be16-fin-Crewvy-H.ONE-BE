package com.crewvy.workspace_service.meeting.dto;

import com.crewvy.workspace_service.meeting.entity.Recording;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import com.crewvy.workspace_service.meeting.entity.VideoConferenceInvitee;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoConferenceListRes {
    private UUID id;
    private UUID hostId;
    private String hostName;
    private String name;
    private String description;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime actualStartTime;
    private String status;
    private Boolean isRecording;
    private List<UUID> inviteeIdList;
    private String recordingUrl;
    private Boolean hasMinute;
    private Integer participantsCnt;

    public static VideoConferenceListRes fromEntity(VideoConference videoConference, String hostName) {
        Optional<Recording> optionalRecording = Optional.ofNullable(videoConference.getRecording());

        return VideoConferenceListRes.builder()
                .id(videoConference.getId())
                .hostId(videoConference.getHostId())
                .hostName(hostName)
                .name(videoConference.getName())
                .description(videoConference.getDescription())
                .scheduledStartTime(videoConference.getScheduledStartTime())
                .actualStartTime(videoConference.getActualStartTime())
                .status(videoConference.getStatus().getCodeName())
                .isRecording(videoConference.getIsRecording().toBoolean())
                .inviteeIdList(videoConference.getVideoConferenceInviteeSet().stream().map(VideoConferenceInvitee::getMemberId).toList())
                .recordingUrl(optionalRecording
                        .map(Recording::getUrl)
                        .orElse(null))
                .hasMinute(optionalRecording
                        .map(Recording::getMinute).isPresent())
                .build();
    }

    public static VideoConferenceListRes fromEntityWithParticipantsCnt(VideoConference videoConference, String hostName, int participantsCnt) {
        Optional<Recording> optionalRecording = Optional.ofNullable(videoConference.getRecording());

        return VideoConferenceListRes.builder()
                .id(videoConference.getId())
                .hostId(videoConference.getHostId())
                .hostName(hostName)
                .name(videoConference.getName())
                .description(videoConference.getDescription())
                .scheduledStartTime(videoConference.getScheduledStartTime())
                .actualStartTime(videoConference.getActualStartTime())
                .status(videoConference.getStatus().getCodeName())
                .isRecording(videoConference.getIsRecording().toBoolean())
                .inviteeIdList(videoConference.getVideoConferenceInviteeSet().stream().map(VideoConferenceInvitee::getMemberId).toList())
                .recordingUrl(optionalRecording
                        .map(Recording::getUrl)
                        .orElse(null))
                .hasMinute(optionalRecording
                        .map(Recording::getMinute).isPresent())
                .participantsCnt(participantsCnt)
                .build();
    }
}
