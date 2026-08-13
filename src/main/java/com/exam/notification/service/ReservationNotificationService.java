package com.exam.notification.service;

import com.exam.auth.dto.UserDTO;
import com.exam.auth.mapper.UserMapper;
import com.exam.reservation.dto.SeatDisplayInfoDTO;
import com.exam.reservation.mapper.ReservationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

// 예매확정/취소 알림 메일 발행 — 회원가입 인증(EmailVerificationServiceImpl)과 같은 SQS 큐를 공유(메시지의
// type 필드로 Lambda가 구분). 알림 발행 실패가 예매/취소 자체를 실패시키면 절대 안 되므로 예외를 전부 삼키고 로그만 남김
@Slf4j
@Service
public class ReservationNotificationService {

    private final SqsClient sqsClient;
    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public ReservationNotificationService(SqsClient sqsClient,
                                           ReservationMapper reservationMapper,
                                           UserMapper userMapper,
                                           ObjectMapper objectMapper,
                                           @Value("${cloud.aws.sqs.notification-queue-url:}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.reservationMapper = reservationMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    public void notifyConfirmed(String userId, Long seatId, Long roundId) {
        publish("RESERVATION_CONFIRMED", userId, seatId, roundId);
    }

    public void notifyCancelled(String userId, Long seatId, Long roundId) {
        publish("RESERVATION_CANCELLED", userId, seatId, roundId);
    }

    private void publish(String type, String userId, Long seatId, Long roundId) {
        if (queueUrl == null || queueUrl.isBlank()) {
            log.warn("NOTIFICATION_QUEUE_URL 미설정 - 알림 스킵 (type={})", type);
            return;
        }
        try {
            UserDTO user = userMapper.findById(userId);
            SeatDisplayInfoDTO info = reservationMapper.findSeatDisplayInfo(seatId, roundId);
            if (user == null || user.getUserEmail() == null || info == null) {
                return;
            }

            Map<String, Object> message = Map.of(
                    "type", type,
                    "email", user.getUserEmail(),
                    "performanceTitle", info.getPTitle(),
                    "roundTime", info.getRoundTime(),
                    "seatInfo", info.getSeatRow() + info.getSeatColume() + "(" + info.getGrade() + ")"
            );
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(objectMapper.writeValueAsString(message))
                    .build());
        } catch (Exception e) {
            log.warn("예매 알림 발행 실패 - type={}, userId={}, seatId={}", type, userId, seatId, e);
        }
    }
}
