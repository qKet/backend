package com.exam.notification.service;

public interface NotificationService {

    boolean isSubscribed(String userId, Long roundId);

    void subscribe(String userId, Long roundId, String clientIp);

    void unsubscribe(String userId, Long roundId, String clientIp);

    // 5분마다 스케줄러가 호출 — open_time이 임박(기본 30분 이내)한 미발송 구독을 찾아 SQS로 publish.
    // best-effort — 한 건이 실패해도 나머지 건 발송을 막지 않음
    void sweepOpenAlerts();
}
