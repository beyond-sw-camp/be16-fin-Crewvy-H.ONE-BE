package com.crewvy.workspace_service.meeting.service;

import com.crewvy.workspace_service.meeting.dto.openvidu.webhook.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenViduWebhookService {

    private final ObjectMapper objectMapper;

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
        log.info("Session created: {}", req);
    }

    private void handleSessionDestroyed(SessionDestroyedReq req) {
        log.info("Session destroyed: {}", req);
    }

    private void handleParticipantJoined(ParticipantJoinedReq req) {
        log.info("Participant joined: {}", req);
    }

    private void handleParticipantLeft(ParticipantLeftReq req) {
        log.info("Participant left: {}", req);
    }

    private void handleWebrtcConnectionCreated(WebrtcConnectionCreatedReq req) {
        log.info("Webrtc connection created: {}", req);
    }

    private void handleWebrtcConnectionDestroyed(WebrtcConnectionDestroyedReq req) {
        log.info("Webrtc connection destroyed: {}", req);
    }

    private void handleRecordingStatusChanged(RecordingStatusChangedReq req) {
        log.info("Recording status changed: {}", req);
    }

    private void handleFilterEventDispatched(FilterEventDispatchedReq req) {
        log.info("Filter event dispatched: {}", req);
    }

    private void handleSignalSent(SignalSentReq req) {
        log.info("Signal sent: {}", req);
    }
}
