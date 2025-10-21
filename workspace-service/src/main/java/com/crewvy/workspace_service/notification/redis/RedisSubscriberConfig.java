package com.crewvy.workspace_service.notification.redis;

import com.crewvy.common.redis.RedisChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisSubscriberConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisMessageSubscriber redisMessageSubscriber;

    public RedisSubscriberConfig(
            @Qualifier("pubSubRedisConnectionFactory") RedisConnectionFactory redisConnectionFactory,
            RedisMessageSubscriber redisMessageSubscriber) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisMessageSubscriber = redisMessageSubscriber;
    }

    @Bean
    public RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(redisMessageSubscriber, new PatternTopic(RedisChannel.NOTIFICATION_CHANNEL));
        return container;
    }
}