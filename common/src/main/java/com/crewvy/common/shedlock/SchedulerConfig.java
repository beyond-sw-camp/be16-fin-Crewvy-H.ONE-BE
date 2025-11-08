package com.crewvy.common.shedlock;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
// defaultLockAtMostFor: 락의 기본 최대 유지 시간 (예: 30초)
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class SchedulerConfig {

    @Bean
    public LockProvider lockProvider(@Qualifier("shedLockConnectionFactory") RedisConnectionFactory connectionFactory) {
        // 두 번째 인자는 Redis Key의 접두사입니다. (기본값: "shedlock")
        return new RedisLockProvider(connectionFactory, "shedlock");
    }
}