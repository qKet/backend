package com.exam.notification.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

// CANCEL_ALERTS + USERS + PERFORMANCE_ROUND + PERFORMANCES + VENUE 조인 결과 — 취소 발생 시 이 회차를
// 구독 중인 수신자한테 보낼 SQS 메시지 payload를 만드는 데 씀 (NotificationServiceImpl.publishCancelAlerts)
@Data
@Alias("CancelAlertRecipientDTO")
public class CancelAlertRecipientDTO {

    private String userEmail;
    private String pTitle;
    private String venueName;
    private LocalDateTime roundTime;
}
