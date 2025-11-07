package com.crewvy.workspace_service.meeting.kafka;

import com.crewvy.common.entity.Bool;
import com.crewvy.common.event.ApprovalCompletedEvent;
import com.crewvy.common.event.MinuteSavedEvent;
import com.crewvy.workspace_service.meeting.entity.MinuteSearchOutboxEvent;
import com.crewvy.workspace_service.meeting.repository.MinuteSearchOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class MinuteSearchOutboxRelayScheduler {

    private final MinuteSearchOutboxEventRepository minuteSearchOutboxEventRepository;
    private final KafkaTemplate<String, Object> minuteSearchEventKafkaTemplate;
    private final ObjectMapper objectMapper;

    public MinuteSearchOutboxRelayScheduler(MinuteSearchOutboxEventRepository minuteSearchOutboxEventRepository
            , @Qualifier("minuteSearchEventKafkaTemplate") KafkaTemplate<String, Object> minuteSearchEventKafkaTemplate
            , ObjectMapper objectMapper) {
        this.minuteSearchOutboxEventRepository = minuteSearchOutboxEventRepository;
        this.minuteSearchEventKafkaTemplate = minuteSearchEventKafkaTemplate;
        this.objectMapper = objectMapper;
    }


    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void relaySearchOutboxEvents() {
        List<MinuteSearchOutboxEvent> events = minuteSearchOutboxEventRepository.findAllByProcessed(Bool.FALSE, Pageable.ofSize(100));

        for (MinuteSearchOutboxEvent event : events) {
            try {
                Object eventPayload;
                String topic = event.getTopic();

                if ("minute-completed-events".equals(topic)) {
                    eventPayload = objectMapper.readValue(event.getPayload(), MinuteSavedEvent.class);
                } else {
                    log.warn("Unknown topic: {}", topic);
                    continue;
                }

                minuteSearchEventKafkaTemplate.send(topic, eventPayload);
                event.setProcessed();
            } catch (Exception e) {
                log.error("Failed to send search outbox event to Kafka: {} Error: {}", event.getId(), e.getMessage());
            }
        }
    }
}