package com.crewvy.workspace_service.meeting.config;

import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitEgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfig {

    private final String LIVEKIT_HOST;
    private final String LIVEKIT_API_KEY;
    private final String LIVEKIT_API_SECRET;

    private final String AWS_ACCESS_KEY;
    private final String AWS_SECRET_KEY;
    private final String AWS_REGION;
    private final String AWS_S3_BUCKET;

    @Autowired
    public LiveKitConfig(@Value("${livekit.host}") String LIVEKIT_HOST,
                         @Value("${livekit.apiKey}") String LIVEKIT_API_KEY,
                         @Value("${livekit.apiSecret}") String LIVEKIT_API_SECRET,
                         @Value("${cloud.aws.credentials.access-key}") String AWS_ACCESS_KEY,
                         @Value("${cloud.aws.credentials.secret-key}") String AWS_SECRET_KEY,
                         @Value("${cloud.aws.region.static}") String AWS_REGION,
                         @Value("${cloud.aws.s3.bucket}") String AWS_S3_BUCKET) {
        this.LIVEKIT_HOST = LIVEKIT_HOST;
        this.LIVEKIT_API_KEY = LIVEKIT_API_KEY;
        this.LIVEKIT_API_SECRET = LIVEKIT_API_SECRET;
        this.AWS_ACCESS_KEY = AWS_ACCESS_KEY;
        this.AWS_SECRET_KEY = AWS_SECRET_KEY;
        this.AWS_REGION = AWS_REGION;
        this.AWS_S3_BUCKET = AWS_S3_BUCKET;
    }

    @Bean
    public RoomServiceClient roomServiceClient() {
        return RoomServiceClient.createClient(LIVEKIT_HOST, LIVEKIT_API_KEY, LIVEKIT_API_SECRET);
    }

    @Bean
    public EgressServiceClient egressServiceClient() {
        return EgressServiceClient.createClient(LIVEKIT_HOST, LIVEKIT_API_KEY, LIVEKIT_API_SECRET);
    }

    @Bean
    public LivekitEgress.S3Upload s3Upload() {
        return LivekitEgress.S3Upload.newBuilder()
                .setAccessKey(AWS_ACCESS_KEY)
                .setSecret(AWS_SECRET_KEY)
                .setRegion(AWS_REGION)
                .setBucket(AWS_S3_BUCKET)
                .build();
    }

    @Bean
    public WebhookReceiver webhookReceiver() {
        return new WebhookReceiver(LIVEKIT_API_KEY, LIVEKIT_API_SECRET);
    }
}
