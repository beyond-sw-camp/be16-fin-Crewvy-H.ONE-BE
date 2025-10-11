package com.crewvy.common.redis;

import com.crewvy.common.entity.Bool;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean(name = "permissionCacheManager")
    public CacheManager permissionCacheManager(@Qualifier("permissionCache") RedisConnectionFactory redisConnectionFactory) {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator
                .builder()
                .allowIfBaseType(Object.class)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);

        // Bool Enum을 위한 커스텀 직렬화/역직렬화 등록
        SimpleModule boolModule = new SimpleModule();
        boolModule.addSerializer(Bool.class, new JsonSerializer<Bool>() {
            @Override
            public void serialize(Bool value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.name()); // Enum 이름을 문자열로 저장
            }
        });
        boolModule.addDeserializer(Bool.class, new JsonDeserializer<Bool>() {
            @Override
            public Bool deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return Bool.valueOf(p.getText()); // 문자열을 Enum 이름으로 변환
            }
        });
        objectMapper.registerModule(boolModule);

        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer))
                .entryTtl(Duration.ofHours(24)); // 캐시 유효 시간을 24시간으로 설정

        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .build();
    }
}
