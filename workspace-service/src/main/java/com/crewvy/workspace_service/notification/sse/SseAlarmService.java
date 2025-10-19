package com.crewvy.workspace_service.notification.sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseAlarmService {

    private final Map<UUID, Set<SseEmitter>> clients = new ConcurrentHashMap<>();

    // 구독 등록
    public SseEmitter subscribe(UUID memberId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        clients.computeIfAbsent(memberId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(memberId, emitter));
        emitter.onTimeout(() -> removeEmitter(memberId, emitter));

        return emitter;
    }

    // 특정 사용자에게 알림 전송
    public void sendToUser(UUID memberId, String type, String message) {
        Set<SseEmitter> emitters = clients.get(memberId);
        if (emitters == null || emitters.isEmpty()) {
            System.out.println("❌ No emitters found for user " + memberId);
            return;
        }

        System.out.println("✅ Sending message to " + memberId + ": " + message);
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(type).data(message));
                System.out.println("➡️ Sent to one emitter");
            } catch (IOException e) {
                System.out.println("⚠️ Failed to send, removing emitter");
                removeEmitter(memberId, emitter);
            }
        });
    }

    private void removeEmitter(UUID memberId, SseEmitter emitter) {
        Set<SseEmitter> emitters = clients.get(memberId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) clients.remove(memberId);
        }
    }

    // 전체 브로드캐스트용 (선택 사항)
    public void sendToAll(String message) {
        clients.values().forEach(set -> set.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(message));
            } catch (IOException e) {
                // 에러 발생 시 제거
                set.remove(emitter);
            }
        }));
    }
}