package com.crewvy.common.kafka;

import com.crewvy.common.event.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SearchEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SearchEventPublisher(@Qualifier("searchEventKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String MEMBER_SAVED_TOPIC = "member-saved-events";
    private static final String MEMBER_DELETED_TOPIC = "member-deleted-events";

    public void publishMemberSaved(MemberSavedEvent event) {
        kafkaTemplate.send(MEMBER_SAVED_TOPIC, event);
    }

    public void publishMemberDeleted(MemberDeletedEvent event) {
        kafkaTemplate.send(MEMBER_DELETED_TOPIC, event);
    }
}
