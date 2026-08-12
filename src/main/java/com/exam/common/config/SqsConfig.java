package com.exam.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

// 취소표 알림(NOTI01_ALERT01)용 SQS. S3Config와 동일 패턴 — 자격증명을 코드에 안 넣고
// IRSA(qket-backend 서비스어카운트)로 SDK 기본 체인이 인증하게 둠.
@Configuration
public class SqsConfig {

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(region))
                .build();
    }
}
