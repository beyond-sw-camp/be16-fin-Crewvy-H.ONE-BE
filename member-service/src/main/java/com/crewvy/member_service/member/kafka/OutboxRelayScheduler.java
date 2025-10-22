package com.crewvy.member_service.member.kafka;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.OutboxEvent;
import com.crewvy.member_service.member.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class OutboxRelayScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // 1. Outbox 리포지토리와 KafkaTemplate을 주입받습니다.
    public OutboxRelayScheduler(OutboxRepository outboxRepository,
                                @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * fixedDelay = 5000 (5초)
     * 이전 작업이 '끝난 시점'으로부터 5초 뒤에 다음 작업을 실행합니다.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional // 2. 이 작업 전체를 하나의 트랜잭션으로 묶습니다.
    public void relayOutboxEvents() {

        // 3. 처리 안 된(processed=false) 이벤트들을 조회합니다. (예: 최대 100개)
        List<OutboxEvent> events = outboxRepository.findAllByProcessed(Bool.FALSE, Pageable.ofSize(100));

        for (OutboxEvent event : events) {
            try {
                // 4. Kafka로 메시지를 전송합니다.
                //    (토픽, MemberId)
                kafkaTemplate.send(event.getTopic(), event.getMemberId().toString());

                // 5. 전송에 성공하면, '처리 완료' 상태로 변경합니다.
                event.setProcessed();

            } catch (Exception e) {
                // 6. Kafka 전송 실패 시! (e.g., Kafka 서버 다운)
                //    로그를 남기고 이 이벤트의 처리를 '다음 주기'로 넘깁니다.
                //    @Transactional이 롤백되거나(설정에 따라 다름)
                //    try-catch로 감쌌기 때문에, processed 상태가 true로 변경되지 않습니다.
                //    따라서 5초 뒤에 '재시도'됩니다.
                log.error("Failed to send outbox event to Kafka: {} Error: {}", event.getId(), e.getMessage());
            }
        }
    }
}