package com.exam.notification.service;

public interface NotificationService {

    boolean isSubscribed(String userId, Long roundId);

    void subscribe(String userId, Long roundId, String clientIp);

    void unsubscribe(String userId, Long roundId, String clientIp);

    // 취소 발생 시 해당 회차 구독자 전원한테 SQS로 알림 메시지 publish. best-effort — 실패해도 예외를 던지지 않음
    // (호출부인 ReservationServiceImpl.cancel()의 예매취소 트랜잭션이 알림 실패로 롤백되면 안 되기 때문)
    void publishCancelAlerts(Long roundId);
}
