package com.crewvy.workspace_service.notification.sse;

import com.crewvy.workspace_service.notification.dto.response.NotificationResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseAlarmService {

    // (1) Spring의 공유 스케줄러를 주입받습니다.
    private final TaskScheduler taskScheduler;

    // (2) Key: memberId, Value: 해당 유저의 SseEmitter 목록
    private final Map<UUID, Set<SseEmitter>> clients = new ConcurrentHashMap<>();

    // (3) Key: SseEmitter, Value: 해당 Emitter의 Ping 작업을 제어하기 위한 객체
    private final Map<SseEmitter, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // 구독 등록
    public SseEmitter subscribe(UUID memberId) {
        // (4) 타임아웃을 매우 길게 설정 (Long.MAX_VALUE)
        SseEmitter emitter = new SseEmitter(60L * 60L * 1000L);

        // (5) 유저의 Emitter 목록에 추가
        clients.computeIfAbsent(memberId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        // (6) 초기 연결 이벤트 전송 (연결 성공 확인용)
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            log.warn("SSE initial connection event failed for {}: {}", memberId, e.getMessage());
            // 연결 시도부터 실패하면 즉시 제거
            removeEmitter(memberId, emitter);
        }

        // (7) 주기적인 Ping(하트비트) 전송 (30초마다)
        // -> 연결이 끊어졌는지 확인하고, 클라이언트의 타임아웃을 방지
        ScheduledFuture<?> pingTask = taskScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("ping").data(System.currentTimeMillis()));
            } catch (Exception e) { // <-- [수정] IOException -> Exception 으로 변경
                // (핵심) 이제 IOException, IllegalStateException 등 모든 오류를
                // 여기서 처리하고 GlobalExceptionHandler로 보내지 않습니다.
                log.warn("SSE ping failed for {}, removing emitter: {}", memberId, e.getMessage());
                removeEmitter(memberId, emitter);
            }
        }, Duration.ofSeconds(30)); // 30초 간격으로 실행

        // (9) Ping 작업을 나중에 취소할 수 있도록 Map에 저장
        scheduledTasks.put(emitter, pingTask);

        // (10) Emitter가 완료되거나 타임아웃되면 cleanup 로직(removeEmitter) 호출
        emitter.onCompletion(() -> {
            log.info("[SSE] completed normally for user {}", memberId);
            removeEmitter(memberId, emitter);
        });
        emitter.onTimeout(() -> {
            log.warn("[SSE] connection timeout for user {}", memberId);
            removeEmitter(memberId, emitter);
        });

        return emitter;
    }

    // 특정 사용자에게 알림 전송
    public void sendToUser(UUID memberId, NotificationResDto message) {
        Set<SseEmitter> emitters = clients.get(memberId);
        if (emitters == null || emitters.isEmpty()) {
            log.warn("No active emitters found for user {}", memberId);
            return;
        }

        log.info("Sending DTO to {} emitters for user {}: {}", emitters.size(), memberId, message.getContents());

        // (주의) forEach 중 removeEmitter가 호출될 수 있으므로,
        // ConcurrentModificationException을 방지하기 위해 복사본을 만들어 순회합니다.
        Set<SseEmitter> emittersCopy = Set.copyOf(emitters);

        emittersCopy.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(message));
                log.info("Sent DTO to one emitter for user {}", memberId);
            } catch (IOException e) {
                // (11) [핵심] DTO 전송 실패 시(연결 끊김), 예외를 잡고 Emitter를 제거합니다.
                log.error("Failed to send DTO to user {}, removing emitter: {}", memberId, e.getMessage());
                removeEmitter(memberId, emitter);
            }
        });
    }

    private void removeEmitter(UUID memberId, SseEmitter emitter) {
        Set<SseEmitter> emitters = clients.get(memberId);
        if (emitters == null || !emitters.remove(emitter)) {
            return; // 이미 제거됨
        }
        ScheduledFuture<?> pingTask = scheduledTasks.remove(emitter);
        if (pingTask != null) pingTask.cancel(true);
        try {
            emitter.complete();
        } catch (Exception ignored) {}
    }

    // 전체 브로드캐스트용 (선택 사항)
    public void sendToAll(String message) {
        clients.forEach((memberId, emitterSet) -> {
            // [수정] ConcurrentModificationException 방지를 위해 복사본을 만들어 순회합니다.
            Set<SseEmitter> emittersCopy = Set.copyOf(emitterSet);

            emittersCopy.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("notification").data(message));
                } catch (IOException e) {
                    // [수정] set.remove() 대신 안전한 removeEmitter 메서드를 호출합니다.
                    log.warn("전체 메시지 전송 실패 (사용자: {}), emitter 제거: {}", memberId, e.getMessage());
                    removeEmitter(memberId, emitter);
                }
            });
        });
    }
}