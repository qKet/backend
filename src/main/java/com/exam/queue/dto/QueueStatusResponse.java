package com.exam.queue.dto;

import com.exam.queue.domain.QueueStatus;

public record QueueStatusResponse(
        String queueToken,
        QueueStatus status,
        long position,
        long estimatedWait
) {
}