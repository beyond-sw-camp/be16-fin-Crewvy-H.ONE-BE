package com.crewvy.common.kafka;

import com.crewvy.common.dto.ScheduleDeleteDto;
import com.crewvy.common.dto.ScheduleDto;
import com.crewvy.common.event.MemberDeletedEvent;
import com.crewvy.common.event.MemberSavedEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchedulePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SchedulePublisher(@Qualifier("scheduleKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String SCHEDULE_SAVED_TOPIC = "schedule-saved-events";
    private static final String SCHEDULE_DELETED_TOPIC = "schedule-deleted-events";

    public void publishScheduleSaved(ScheduleDto event) {
        kafkaTemplate.send(SCHEDULE_SAVED_TOPIC, event);
    }

    public void publishScheduleDeleted(ScheduleDeleteDto event) {
        kafkaTemplate.send(SCHEDULE_DELETED_TOPIC, event);
    }
}