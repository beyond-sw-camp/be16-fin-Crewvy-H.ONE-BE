package com.crewvy.workspace_service.meeting.controller;

import com.crewvy.workspace_service.meeting.service.LiveKitWebhookService;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitWebhook.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/livekit")
public class LiveKitController {

    private final WebhookReceiver webhookReceiver;
    private final LiveKitWebhookService liveKitWebhookService;

    @PostMapping(value = "/webhook", consumes = "application/webhook+json")
    public ResponseEntity<?> handleWebhook(@RequestHeader("Authorization") String authHeader, @RequestBody String body) {

        WebhookEvent event = webhookReceiver.receive(body, authHeader);
        liveKitWebhookService.handleWebhook(event);

        return ResponseEntity.ok().build();
    }
}