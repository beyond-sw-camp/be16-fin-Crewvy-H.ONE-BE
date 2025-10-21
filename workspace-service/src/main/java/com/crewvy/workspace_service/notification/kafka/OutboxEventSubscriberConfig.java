package com.crewvy.workspace_service.notification.kafka;

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

@Configuration
public class OutboxEventSubscriberConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // 1. String 값을 받는 ConsumerFactory 정의
    @Bean
    public ConsumerFactory<String, String> outboxEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId); // yml의 group-id 사용
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Member-Service가 String으로 보냈으므로, StringDeserializer로 받음
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // 2. 기존 kafkaListenerContainerFactory와 충돌을 피하기 위해 이름을 변경합니다.
    @Bean("outboxEventKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> outboxEventKafkaListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        // 1번에서 만든 String용 팩토리를 연결
        factory.setConsumerFactory(outboxEventConsumerFactory());
        return factory;
    }
}