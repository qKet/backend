package com.exam.queue.dto;

import com.exam.queue.domain.QueueStatus;

/**
 * remainingSeconds: 입장(ENTERED) 상태에서 좌석 선택에 쓸 수 있는 잔여 시간(초) — 좌석 화면
 * 카운트다운 표시용. WAITING/EXPIRED에서는 의미가 없어 0.
 */
public record QueueStatusResponse(
        String queueToken,
        QueueStatus status,
        long position,
        long estimatedWait,
        long remainingSeconds
) {
}