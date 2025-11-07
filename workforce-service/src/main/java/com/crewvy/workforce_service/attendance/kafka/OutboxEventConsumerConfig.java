package com.crewvy.workforce_service.attendance.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbox Pattern 이벤트 구독 설정
 * Member-Service의 member-create 토픽을 구독하여 신규 직원 생성 이벤트 처리
 */
@Configuration
public class OutboxEventConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * String payload를 받는 Kafka Consumer Factory
     * Member-Service가 memberId를 String으로 전송
     */
    @Bean
    public ConsumerFactory<String, String> outboxEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Outbox 이벤트 전용 Kafka Listener Container Factory
     * 기존 kafkaListenerContainerFactory와 충돌 방지를 위해 별도 이름 사용
     */
    @Bean("outboxEventKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> outboxEventKafkaListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(outboxEventConsumerFactory());
        return factory;
    }
}
