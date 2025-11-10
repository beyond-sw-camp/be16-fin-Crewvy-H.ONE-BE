package com.crewvy.workspace_service.calendar.kafka;

import com.crewvy.common.dto.ScheduleDeleteDto;
import com.crewvy.common.dto.ScheduleDto;
import com.crewvy.workspace_service.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleListener {

    private final CalendarService calendarService;

    /**
     * 'schedule-saved-events' 토픽을 구독합니다.
     * 메시지는 ScheduleDto 타입으로 자동 변환됩니다.
     */
    @KafkaListener(
            topics = "schedule-saved-events",
            groupId = "schedule-saved-consumer-group", // 이 리스너만의 고유 그룹 ID
            containerFactory = "scheduleKafkaListenerFactory" // (1) 위에서 만든 팩토리 지정
    )
    public void handleScheduleSaved(ScheduleDto event) { // (2) DTO 타입을 정확히 지정
        log.info("Schedule Saved 이벤트 수신: {}", event.toString());

        calendarService.saveSchedule(event);
    }

    /**
     * 'schedule-deleted-events' 토픽을 구독합니다.
     * 메시지는 ScheduleDeleteDto 타입으로 자동 변환됩니다.
     */
    @KafkaListener(
            topics = "schedule-deleted-events",
            groupId = "schedule-deleted-consumer-group", // 이 리스너만의 고유 그룹 ID
            containerFactory = "scheduleKafkaListenerFactory" // (1) 위에서 만든 팩토리 지정
    )
    public void handleScheduleDeleted(ScheduleDeleteDto event) { // (2) DTO 타입을 정확히 지정
        log.info("Schedule Deleted 이벤트 수신: {}", event.toString());

        calendarService.deleteSchedule(event);
    }
}