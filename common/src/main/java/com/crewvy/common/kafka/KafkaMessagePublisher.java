package com.crewvy.common.kafka;

import com.crewvy.common.dto.NotificationMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessagePublisher {

    private final KafkaTemplate<String, NotificationMessage> kafkaTemplate;

    public KafkaMessagePublisher(KafkaTemplate<String, NotificationMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, NotificationMessage message) {
        kafkaTemplate.send(topic, message);
    }
}