package com.crewvy.workspace_service.meeting.controller;

import io.openvidu.java.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class OpenViduController {

    @Autowired
    private OpenVidu openVidu;

    private Map<String, Session> sessions = new HashMap<>();

    @PostMapping("/{sessionId}")
    public String initializeSession(@PathVariable("sessionId") String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) return session.getSessionId();

        try {
            session = openVidu.createSession();
            sessions.put(session.getSessionId(), session);
            return session.getSessionId();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            e.printStackTrace();
            return "Error initializing session";
        }
    }

    @PostMapping("/{sessionId}/connections")
    public String generateToken(@PathVariable String sessionId) {
        try {
            // 세션 ID로 토큰 생성
            if (!sessions.containsKey(sessionId)) {
                return "Session not found";
            }

            Session session = sessions.get(sessionId);
            ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                    .type(ConnectionType.WEBRTC)
                    .build();
            String token = session.createConnection(connectionProperties).getToken();
            return token; // 클라이언트가 사용할 토큰 반환
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            e.printStackTrace();
            return "Error generating token";
        }
    }
}