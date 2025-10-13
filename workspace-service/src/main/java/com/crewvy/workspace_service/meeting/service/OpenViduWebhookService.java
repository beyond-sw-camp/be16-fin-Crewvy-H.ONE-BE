package com.crewvy.workspace_service.meeting.service;

import com.crewvy.workspace_service.meeting.dto.openvidu.webhook.*;
import com.crewvy.workspace_service.meeting.repository.VideoConferenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OpenViduWebhookService {

    private final ObjectMapper objectMapper;
    private final VideoConferenceRepository videoConferenceRepository;

    public void handleOpenViduWebhook(JsonNode body) {
        String event = body.get("event").asText();

        try {
            switch (event) {
                case "sessionCreated" -> handleSessionCreated(objectMapper.treeToValue(body, SessionCreatedReq.class));
                case "sessionDestroyed" -> handleSessionDestroyed(objectMapper.treeToValue(body, SessionDestroyedReq.class));
                case "participantJoined" -> handleParticipantJoined(objectMapper.treeToValue(body, ParticipantJoinedReq.class));
                case "participantLeft" -> handleParticipantLeft(objectMapper.treeToValue(body, ParticipantLeftReq.class));
                case "webrtcConnectionCreated" -> handleWebrtcConnectionCreated(objectMapper.treeToValue(body, WebrtcConnectionCreatedReq.class));
                case "webrtcConnectionDestroyed" -> handleWebrtcConnectionDestroyed(objectMapper.treeToValue(body, WebrtcConnectionDestroyedReq.class));
                case "recordingStatusChanged" -> handleRecordingStatusChanged(objectMapper.treeToValue(body, RecordingStatusChangedReq.class));
                case "filterEventDispatched" -> handleFilterEventDispatched(objectMapper.treeToValue(body, FilterEventDispatchedReq.class));
                case "signalSent" -> handleSignalSent(objectMapper.treeToValue(body, SignalSentReq.class));
            }
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleSessionCreated(SessionCreatedReq req) {
        log.info("OpenViduWebhook - Session created: {}", req);
    }

    private void handleSessionDestroyed(SessionDestroyedReq req) {
        log.info("OpenViduWebhook - Session destroyed: {}", req);
        videoConferenceRepository.findBySessionId(req.getSessionId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."))
                .endVideoConference();
    }

    private void handleParticipantJoined(ParticipantJoinedReq req) {
        log.info("OpenViduWebhook - Participant joined: {}", req);
    }

    private void handleParticipantLeft(ParticipantLeftReq req) {
        log.info("OpenViduWebhook - Participant left: {}", req);
    }

    private void handleWebrtcConnectionCreated(WebrtcConnectionCreatedReq req) {
        log.info("OpenViduWebhook - Webrtc connection created: {}", req);
    }

    private void handleWebrtcConnectionDestroyed(WebrtcConnectionDestroyedReq req) {
        log.info("OpenViduWebhook - Webrtc connection destroyed: {}", req);
    }

    private void handleRecordingStatusChanged(RecordingStatusChangedReq req) {
        log.info("OpenViduWebhook - Recording status changed: {}", req);
    }

    private void handleFilterEventDispatched(FilterEventDispatchedReq req) {
        log.info("OpenViduWebhook - Filter event dispatched: {}", req);
    }

    private void handleSignalSent(SignalSentReq req) {
        log.info("OpenViduWebhook - Signal sent: {}", req);
    }
}
