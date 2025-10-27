package com.crewvy.workspace_service.notification.sse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfig {

    /**
     * SseAlarmService에서 주입받을 수 있도록
     * TaskScheduler Bean을 명시적으로 등록합니다.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // (1) 스케줄러 스레드 풀 크기 설정 (SSE Ping 전용)
        scheduler.setPoolSize(10);

        // (2) 스레드 이름 접두사 (로그 볼 때 유용함)
        scheduler.setThreadNamePrefix("sse-ping-pool-");

        // (3) 스케줄러 초기화
        scheduler.initialize();

        return scheduler;
    }
}