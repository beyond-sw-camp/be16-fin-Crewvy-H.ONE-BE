package com.crewvy.workspace_service.meeting.service;

import com.crewvy.workspace_service.meeting.dto.ai.TranscribeReq;
import com.crewvy.workspace_service.meeting.entity.Recording;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import com.crewvy.workspace_service.meeting.repository.RecordingRepository;
import com.crewvy.workspace_service.meeting.repository.VideoConferenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import livekit.LivekitEgress.EgressStatus;
import livekit.LivekitEgress.FileInfo;
import livekit.LivekitWebhook.WebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
public class LiveKitWebhookService {

    private final VideoConferenceRepository videoConferenceRepository;
    private final RecordingRepository recordingRepository;
    private final KafkaTemplate<String, String> meetingKafkaTemplate;
    private final ObjectMapper objectMapper;

    public LiveKitWebhookService(VideoConferenceRepository videoConferenceRepository,
                                 RecordingRepository recordingRepository,
                                 KafkaTemplate<String, String> meetingKafkaTemplate,
                                 ObjectMapper objectMapper) {

        this.videoConferenceRepository = videoConferenceRepository;
        this.recordingRepository = recordingRepository;
        this.meetingKafkaTemplate = meetingKafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void handleWebhook(WebhookEvent event) {
        switch (event.getEvent()) {
            case "room_started" -> handleRoomStarted(event);
            case "room_finished" -> handleRoomFinished(event);
            case "participant_joined" -> handleParticipantJoined(event);
            case "participant_left" -> handleParticipantLeft(event);
            case "participant_connection_aborted" -> handleParticipantConnectionAborted(event);
            case "track_published" -> handleTrackPublished(event);
            case "track_unpublished" -> handleTrackUnpublished(event);
            case "egress_started" -> handleEgressStarted(event);
            case "egress_updated" -> handleEgressUpdated(event);
            case "egress_ended" -> handleEgressEnded(event);
            case "ingress_started" -> handleIngressStarted(event);
            case "ingress_ended" -> handleIngressEnded(event);
        }
    }

    private void handleRoomStarted(WebhookEvent event) {
        log.info("LiveKit-Webhook(room_started) - {}", event.getRoom());
    }


    private void handleRoomFinished(WebhookEvent event) {
        log.info("LiveKit_Webhook(room_finished) - {}", event.getRoom());

        VideoConference videoConference = videoConferenceRepository.findById(UUID.fromString(event.getRoom().getName()))
                .orElseThrow(() -> new EntityNotFoundException("회의를 찾을 수 없습니다."));

        videoConference.endVideoConference();
    }

    private void handleParticipantJoined(WebhookEvent event) {
        log.info("LiveKit_Webhook(participant_joined) - {}", event.getRoom());
        log.info("LiveKit_Webhook(participant_joined) - {}", event.getParticipant());
    }

    private void handleParticipantLeft(WebhookEvent event) {
        log.info("LiveKit_Webhook(participant_left) - {}", event.getRoom());
        log.info("LiveKit_Webhook(participant_left) - {}", event.getParticipant());
    }

    private void handleParticipantConnectionAborted(WebhookEvent event) {
        log.info("LiveKit_Webhook(participant_connection_aborted) - {}", event.getRoom());
        log.info("LiveKit_Webhook(participant_connection_aborted) - {}", event.getParticipant());
    }

    private void handleTrackPublished(WebhookEvent event) {
        log.info("LiveKit_Webhook(track_published) - {}", event.getRoom());
        log.info("LiveKit_Webhook(track_published) - {}", event.getParticipant());
        log.info("LiveKit_Webhook(track_published) - {}", event.getTrack());
    }

    private void handleTrackUnpublished(WebhookEvent event) {
        log.info("LiveKit_Webhook(track_unpublished) - {}", event.getRoom());
        log.info("LiveKit_Webhook(track_unpublished) - {}", event.getParticipant());
        log.info("LiveKit_Webhook(track_unpublished) - {}", event.getTrack());
    }

    private void handleEgressStarted(WebhookEvent event) {
        log.info("LiveKit_Webhook(egress_started) - {}", event.getEgressInfo());
    }

    private void handleEgressUpdated(WebhookEvent event) {
        log.info("LiveKit_Webhook(egress_updated) - {}", event.getEgressInfo());
    }

    private void handleEgressEnded(WebhookEvent event) {
        log.info("LiveKit_Webhook(egress_ended) - {}", event.getEgressInfo());
        if (event.getEgressInfo().getStatus() != EgressStatus.EGRESS_COMPLETE) return;

        FileInfo fileInfo = event.getEgressInfo().getFileResults(0);

        VideoConference videoConference = videoConferenceRepository.findById(UUID.fromString(event.getEgressInfo().getRoomName()))
                .orElseThrow(() -> new EntityNotFoundException("없는 화상회의 입니다."));

        Recording recording = Recording.fromFileInfo(fileInfo, videoConference);
        recordingRepository.save(recording);

        TranscribeReq transcribeReq = TranscribeReq.fromEntity(recording);

        String data;
        try {
            data = objectMapper.writeValueAsString(transcribeReq);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException("오류 발생");
        }

        meetingKafkaTemplate.send("transcribe-request", data);
    }

    private void handleIngressStarted(WebhookEvent event) {
        log.info("LiveKit_Webhook(ingress_started) - {}", event.getIngressInfo());
    }

    private void handleIngressEnded(WebhookEvent event) {
        log.info("LiveKit_Webhook(ingress_ended) - {}", event.getIngressInfo());
    }
}
