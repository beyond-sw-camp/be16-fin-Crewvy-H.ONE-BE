package com.crewvy.workspace_service.meeting.controller;

import com.crewvy.workspace_service.meeting.service.OpenViduWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/openvidu-webhooks")
public class OpenViduWebhookController {

    private final OpenViduWebhookService openViduWebhookService;

    @PostMapping("")
    public ResponseEntity<?> handleOpenViduWebhook(@RequestBody JsonNode body) {
        openViduWebhookService.handleOpenViduWebhook(body);
        return ResponseEntity.ok().build();
    }
}