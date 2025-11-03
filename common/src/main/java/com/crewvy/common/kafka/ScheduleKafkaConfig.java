package com.crewvy.common.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(value = "spring.kafka.enabled", havingValue = "true")
@EnableKafka
public class ScheduleKafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean // 이 Bean의 이름은 "scheduleDtoProducerFactory"가 됩니다.
    public ProducerFactory<String, Object> scheduleDtoProducerFactory(ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(objectMapper);
        jsonSerializer.setAddTypeInfo(true);

        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), jsonSerializer);
    }

    @Bean(name = "scheduleKafkaTemplate")
    public KafkaTemplate<String, Object> schedulekafkaTemplate(
            // (2) [수정] @Qualifier를 사용해서 2개 중 어떤 Factory를 쓸지 명시
            @Qualifier("scheduleDtoProducerFactory")
            ProducerFactory<String, Object> producerFactory) { // (파라미터 이름은 자유롭게)

        return new KafkaTemplate<>(producerFactory);
    }
}
