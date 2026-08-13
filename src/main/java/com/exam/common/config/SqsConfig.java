package com.exam.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

// 알림 발행용 SQS 공용 클라이언트 — 회원가입 이메일 인증/예매확정·취소 알림과 예매 오픈 알림이
// 이 Bean을 같이 씀(큐는 서로 다름, 클라이언트만 공유).
// S3Config와 동일 패턴 — 자격증명을 코드에 안 넣고 IRSA(qket-backend 서비스어카운트)로 SDK 기본 체인이 인증하게 둠.
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
