package com.crewvy.workspace_service.meeting.config;

import io.openvidu.java.client.OpenVidu;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenViduConfig {

    @Value("${openvidu.url}")
    private String OPENVIDU_URL;

    @Value("${openvidu.secret}")
    private String OPENVIDU_SECRET;

    @Bean
    public OpenVidu openVidu() {
        return new OpenVidu(OPENVIDU_URL, OPENVIDU_SECRET);
    }
}
