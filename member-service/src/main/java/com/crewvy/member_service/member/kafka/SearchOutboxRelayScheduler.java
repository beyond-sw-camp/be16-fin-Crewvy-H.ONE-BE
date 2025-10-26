package com.crewvy.member_service.member.kafka;

import com.crewvy.common.entity.Bool;
import com.crewvy.common.event.MemberSavedEvent;
import com.crewvy.member_service.member.entity.SearchOutboxEvent;
import com.crewvy.member_service.member.repository.SearchOutboxEventRepository;
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
public class SearchOutboxRelayScheduler {

    private final SearchOutboxEventRepository searchOutboxEventRepository;
    private final KafkaTemplate<String, Object> memberSearchEventKafkaTemplate;
    private final ObjectMapper objectMapper;

    public SearchOutboxRelayScheduler(SearchOutboxEventRepository searchOutboxEventRepository,
                                      @Qualifier("memberSearchEventKafkaTemplate") KafkaTemplate<String, Object> memberSearchEventKafkaTemplate,
                                      ObjectMapper objectMapper) {
        this.searchOutboxEventRepository = searchOutboxEventRepository;
        this.memberSearchEventKafkaTemplate = memberSearchEventKafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void relaySearchOutboxEvents() {
        List<SearchOutboxEvent> events = searchOutboxEventRepository.findAllByProcessed(Bool.FALSE, Pageable.ofSize(100));

        for (SearchOutboxEvent event : events) {
            try {
                MemberSavedEvent memberSavedEvent = objectMapper.readValue(event.getPayload(), MemberSavedEvent.class);
                memberSearchEventKafkaTemplate.send(event.getTopic(), memberSavedEvent);
                event.setProcessed();
            } catch (Exception e) {
                log.error("Failed to send search outbox event to Kafka: {} Error: {}", event.getId(), e.getMessage());
            }
        }
    }
}
