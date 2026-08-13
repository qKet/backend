package com.exam.notification.dto;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

// OPEN_ALERTS + USERS + PERFORMANCE_ROUND + PERFORMANCES + VENUE 조인 결과 — 예매 오픈 30분 전
// 발송 대상(NotificationServiceImpl의 스케줄러)한테 보낼 SQS 메시지 payload를 만드는 데 씀
@Data
@Alias("OpenAlertRecipientDTO")
public class OpenAlertRecipientDTO {

    private Long alertId;
    private String userEmail;
    private String pTitle;
    private String venueName;
    private LocalDateTime openTime;
    private LocalDateTime roundTime;
}
