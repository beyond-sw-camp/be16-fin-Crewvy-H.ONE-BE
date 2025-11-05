package com.crewvy.common.aes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "aes.enabled", havingValue = "true")
public class AESConfig {

    @Value("${aes.key}")
    private String key;

    @Bean
    public AESUtil aesUtil() {
        return new AESUtil(key);
    }
}
