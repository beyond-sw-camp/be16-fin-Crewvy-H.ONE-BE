package com.crewvy.common.notification;

import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.common.dto.ScheduleDto;
import com.crewvy.common.kafka.KafkaMessagePublisher;
import com.crewvy.common.kafka.SchedulePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    // (1) 실제 Kafka 전송기 주입
    private final KafkaMessagePublisher messagePublisher;
    private final SchedulePublisher schedulePublisher;

    /**
     * NotificationMessage 타입의 이벤트를 수신하는 범용 리스너
     * * (2) @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
     * - 이벤트를 발행한 Service의 트랜잭션(@Transactional)이
     * - '성공적으로 커밋(COMMIT)'되었을 때만 이 메서드를 실행합니다.
     * - 롤백(ROLLBACK)되면 이 메서드는 절대 실행되지 않습니다. (유령 알림 방지)
     * * (3) @Async
     * - 이 알림 발송 로직을 별도 스레드(비동기)로 실행합니다.
     * - Service의 API 응답이 Kafka 전송을 기다리지 않고 즉시 반환됩니다. (성능 향상)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleNotificationMessage(NotificationMessage message) {

        // (4) DB 커밋이 보장된 상태에서 Kafka로 안전하게 발송
        try {
            messagePublisher.publish("notification", message);
            log.info("알림 이벤트 Kafka 전송 성공: {}", message.getMemberId());

        } catch (Exception e) {
            // (5) Kafka 전송이 실패하더라도, DB 트랜잭션은 이미 성공했으므로
            //     롤백되지 않습니다. 로그만 남깁니다.
            log.error("Kafka 메시지 발송 실패. (알림 누락 발생): {}", message, e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void scheduleSaved(ScheduleDto message) {

        try {
            schedulePublisher.publishScheduleSaved(message);
            log.info("일정 Kafka 전송 성공: {}", message.getMemberId());

        } catch (Exception e) {
            // (5) Kafka 전송이 실패하더라도, DB 트랜잭션은 이미 성공했으므로
            //     롤백되지 않습니다. 로그만 남깁니다.
            log.error("Kafka 메시지 발송 실패. (일정 누락 발생): {}", message, e);
        }
    }
}