package com.exam.notification.service;

import com.exam.notification.dto.CancelAlertDTO;
import com.exam.notification.dto.CancelAlertRecipientDTO;
import com.exam.notification.mapper.CancelAlertMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 파일명: NotificationServiceImpl.java
 *
 **/
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final CancelAlertMapper cancelAlertMapper;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.sqs.cancel-alert-queue-url:}")
    private String cancelAlertQueueUrl;

    public NotificationServiceImpl(CancelAlertMapper cancelAlertMapper, SqsClient sqsClient, ObjectMapper objectMapper) {
        this.cancelAlertMapper = cancelAlertMapper;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    /***********************************
     *  이름      :   isSubscribed
     *  기능      :   로그인 사용자의 해당 회차 취소표 알림 구독 상태 조회
     *  param    :   String userId, Long roundId
     *  return   :   boolean
     ************************************/
    @Override
    public boolean isSubscribed(String userId, Long roundId) {
        return "Y".equals(cancelAlertMapper.findUseYn(userId, roundId));
    }

    /***********************************
     *  이름      :   subscribe
     *  기능      :   취소표 알림 구독 켜기
     *  param    :   String userId, Long roundId, String clientIp
     ************************************/
    @Override
    public void subscribe(String userId, Long roundId, String clientIp) {
        upsert(userId, roundId, "Y", clientIp);
    }

    /***********************************
     *  이름      :   unsubscribe
     *  기능      :   취소표 알림 구독 끄기
     *  param    :   String userId, Long roundId, String clientIp
     ************************************/
    @Override
    public void unsubscribe(String userId, Long roundId, String clientIp) {
        upsert(userId, roundId, "N", clientIp);
    }

    private void upsert(String userId, Long roundId, String useYn, String clientIp) {
        CancelAlertDTO dto = new CancelAlertDTO();
        dto.setUserId(userId);
        dto.setRoundId(roundId);
        dto.setUseYn(useYn);
        dto.setInsId(userId);
        dto.setInsIp(clientIp);
        cancelAlertMapper.upsert(dto);
    }

    /***********************************
     *  이름      :   publishCancelAlerts
     *  기능      :   취소 발생 회차의 구독자 전원에게 SQS 메시지 publish(수신자당 1건) — Lambda가 이 큐를 구독해서 SES로 발송함.
     *              큐 URL 미설정(로컬 등) 또는 publish 실패는 로그만 남기고 삼킴 — 예매 취소 자체를 막으면 안 됨
     *  param    :   Long roundId
     ************************************/
    @Override
    public void publishCancelAlerts(Long roundId) {
        if (!StringUtils.hasText(cancelAlertQueueUrl)) {
            log.warn("CANCEL_ALERT_QUEUE_URL이 설정되지 않아 취소표 알림을 건너뜁니다. roundId={}", roundId);
            return;
        }
        try {
            List<CancelAlertRecipientDTO> recipients = cancelAlertMapper.findActiveSubscribersByRoundId(roundId);
            for (CancelAlertRecipientDTO recipient : recipients) {
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(cancelAlertQueueUrl)
                        .messageBody(toMessageBody(recipient))
                        .build());
            }
        } catch (Exception e) {
            log.error("취소표 알림 SQS publish 실패. roundId={}", roundId, e);
        }
    }

    private String toMessageBody(CancelAlertRecipientDTO recipient) throws JsonProcessingException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("toEmail", recipient.getUserEmail());
        payload.put("pTitle", recipient.getPTitle());
        payload.put("venueName", recipient.getVenueName());
        payload.put("roundTime", recipient.getRoundTime());
        return objectMapper.writeValueAsString(payload);
    }
}
