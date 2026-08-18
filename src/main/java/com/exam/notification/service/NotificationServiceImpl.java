package com.exam.notification.service;

import com.exam.notification.dto.OpenAlertDTO;
import com.exam.notification.dto.OpenAlertRecipientDTO;
import com.exam.notification.mapper.OpenAlertMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;
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

    private final OpenAlertMapper openAlertMapper;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.sqs.open-alert-queue-url:}")
    private String openAlertQueueUrl;

    // "예매 오픈 몇 분 전"에 보낼지 — 기본 30분. application.yml에서 바꿀 수 있게 열어둠
    @Value("${notification.open-alert-minutes-before:30}")
    private int minutesBefore;

    public NotificationServiceImpl(OpenAlertMapper openAlertMapper, SqsClient sqsClient, ObjectMapper objectMapper) {
        this.openAlertMapper = openAlertMapper;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    /***********************************
     *  이름      :   isSubscribed
     *  기능      :   로그인 사용자의 해당 회차 예매 오픈 알림 구독 상태 조회
     *  param    :   String userId, Long roundId
     *  return   :   boolean
     ************************************/
    @Override
    public boolean isSubscribed(String userId, Long roundId) {
        return "Y".equals(openAlertMapper.findUseYn(userId, roundId));
    }

    /***********************************
     *  이름      :   subscribe
     *  기능      :   예매 오픈 알림 구독 켜기
     *  param    :   String userId, Long roundId, String clientIp
     ************************************/
    @Override
    public void subscribe(String userId, Long roundId, String clientIp) {
        upsert(userId, roundId, "Y", clientIp);
    }

    /***********************************
     *  이름      :   unsubscribe
     *  기능      :   예매 오픈 알림 구독 끄기
     *  param    :   String userId, Long roundId, String clientIp
     ************************************/
    @Override
    public void unsubscribe(String userId, Long roundId, String clientIp) {
        upsert(userId, roundId, "N", clientIp);
    }

    private void upsert(String userId, Long roundId, String useYn, String clientIp) {
        OpenAlertDTO dto = new OpenAlertDTO();
        dto.setUserId(userId);
        dto.setRoundId(roundId);
        dto.setUseYn(useYn);
        dto.setInsId(userId);
        dto.setInsIp(clientIp);
        openAlertMapper.upsert(dto);
    }

    /***********************************
     *  이름      :   sweepOpenAlerts
     *  기능      :   5분마다 실행 — open_time이 임박한 미발송 구독을 찾아 SQS에 publish(구독 1건당 메시지 1건).
     *              Lambda가 이 큐를 구독해서 SES로 발송함. 개별 건 실패는 로그만 남기고 삼킴 — 한 건 실패가
     *              나머지 구독자 발송을 막으면 안 됨. 큐 URL 미설정(로컬 등)이면 실제 publish 대신 무엇을
     *              보냈을지 로그로만 남김(dry-run) — notified_yn은 안 건드려서 나중에 큐가 생기면 그때 진짜 발송됨
     ************************************/
    @Override
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void sweepOpenAlerts() {
        boolean queueConfigured = StringUtils.hasText(openAlertQueueUrl);
        // DB 서버(RDS)의 NOW()가 아니라 JVM 시각(TZ=Asia/Seoul 고정됨)을 기준으로 윈도우를 계산해서 넘김 —
        // RDS 시간대가 UTC라 DB의 NOW()를 그대로 쓰면 KST로 저장된 open_time과 9시간 어긋나서 항상 빈 결과가 나왔음(2026-08-18 발견)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(minutesBefore);
        List<OpenAlertRecipientDTO> dueAlerts = openAlertMapper.findDueAlerts(now, windowEnd);
        if (dueAlerts.isEmpty()) {
            return;
        }
        if (!queueConfigured) {
            log.warn("OPEN_ALERT_QUEUE_URL이 설정되지 않아 {}건을 실제 발송 없이 로그로만 남깁니다(dry-run).", dueAlerts.size());
        }
        for (OpenAlertRecipientDTO alert : dueAlerts) {
            try {
                String messageBody = toMessageBody(alert);
                if (queueConfigured) {
                    sqsClient.sendMessage(SendMessageRequest.builder()
                            .queueUrl(openAlertQueueUrl)
                            .messageBody(messageBody)
                            .build());
                    openAlertMapper.markNotified(alert.getAlertId());
                    log.info("예매 오픈 알림 SQS publish 성공. alertId={}, to={}", alert.getAlertId(), alert.getUserEmail());
                } else {
                    log.info("[DRY-RUN] 예매 오픈 알림 발송 대상: {}", messageBody);
                }
            } catch (Exception e) {
                log.error("예매 오픈 알림 처리 실패. alertId={}", alert.getAlertId(), e);
            }
        }
    }

    private String toMessageBody(OpenAlertRecipientDTO alert) throws JsonProcessingException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("toEmail", alert.getUserEmail());
        payload.put("pTitle", alert.getPTitle());
        payload.put("venueName", alert.getVenueName());
        payload.put("openTime", alert.getOpenTime());
        payload.put("roundTime", alert.getRoundTime());
        return objectMapper.writeValueAsString(payload);
    }
}
