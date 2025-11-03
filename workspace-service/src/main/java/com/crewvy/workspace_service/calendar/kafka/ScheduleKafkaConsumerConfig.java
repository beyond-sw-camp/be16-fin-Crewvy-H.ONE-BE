package com.crewvy.workspace_service.calendar.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@ConditionalOnProperty(value = "spring.kafka.enabled", havingValue = "true")
public class ScheduleKafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> scheduleConsumerFactory(
            ObjectMapper objectMapper) { // (2) Spring의 ObjectMapper를 주입받음

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class); // (3) 이 줄 제거

        // (4) 주입받은 ObjectMapper로 Deserializer 인스턴스 생성
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(objectMapper);

        // (5) [핵심] Producer가 보낸 타입 헤더를 '읽도록' 설정
        jsonDeserializer.setUseTypeHeaders(true);
        // (6) [핵심] 신뢰하는 DTO 패키지 설정
        jsonDeserializer.addTrustedPackages("com.crewvy.common.dto");

        // (7) configProps와 Deserializer 인스턴스를 함께 넘김
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean(name = "scheduleKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> scheduleKafkaListenerFactory(
            ConsumerFactory<String, Object> scheduleConsumerFactory) { // (8) 팩토리를 주입받도록 변경

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(scheduleConsumerFactory);
        return factory;
    }
}