package com.crewvy.workforce_service.approval.kafka;

import com.crewvy.common.entity.Bool;
import com.crewvy.common.event.ApprovalCompletedEvent;
import com.crewvy.workforce_service.approval.entity.ApprovalSearchOutboxEvent;
import com.crewvy.workforce_service.approval.repository.ApprovalSearchOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class ApprovalSearchOutboxRelayScheduler {

    private final ApprovalSearchOutboxEventRepository approvalSearchOutboxEventRepository;
    private final KafkaTemplate<String, Object> approvalSearchEventKafkaTemplate;
    private final ObjectMapper objectMapper;

    public ApprovalSearchOutboxRelayScheduler(ApprovalSearchOutboxEventRepository approvalSearchOutboxEventRepository
            , @Qualifier("approvalSearchEventKafkaTemplate") KafkaTemplate<String, Object> approvalSearchEventKafkaTemplate
            , ObjectMapper objectMapper) {
        this.approvalSearchOutboxEventRepository = approvalSearchOutboxEventRepository;
        this.approvalSearchEventKafkaTemplate = approvalSearchEventKafkaTemplate;
        this.objectMapper = objectMapper;
    }


    @Scheduled(fixedDelay = 5000)
    @Transactional
    @SchedulerLock(
            name = "relaySearchApprovalOutboxEvents", // ★ 중요: 작업별로 고유한 이름 지정
            lockAtMostFor = "PT10M",  // 작업이 10분 이상 걸리면 강제 잠금 해제
            lockAtLeastFor = "PT30S"  // 작업이 빨리 끝나도 최소 30초간 잠금 유지
    )
    public void relaySearchOutboxEvents() {
        List<ApprovalSearchOutboxEvent> events = approvalSearchOutboxEventRepository.findAllByProcessed(Bool.FALSE, Pageable.ofSize(100));

        for (ApprovalSearchOutboxEvent event : events) {
            try {
                Object eventPayload;
                String topic = event.getTopic();

                if ("approval-completed-events".equals(topic)) {
                    eventPayload = objectMapper.readValue(event.getPayload(), ApprovalCompletedEvent.class);
                } else {
                    log.warn("Unknown topic: {}", topic);
                    continue;
                }

                approvalSearchEventKafkaTemplate.send(topic, eventPayload);
                event.setProcessed();
            } catch (Exception e) {
                log.error("Failed to send search outbox event to Kafka: {} Error: {}", event.getId(), e.getMessage());
            }
        }
    }
}