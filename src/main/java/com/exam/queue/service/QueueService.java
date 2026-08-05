package com.exam.queue.service;

import com.exam.queue.dto.QueueJoinResponse;
import com.exam.queue.dto.QueueStatusResponse;

public interface QueueService {

    QueueJoinResponse join(
            Long scheduleId,
            String userId
    );

    QueueStatusResponse getStatus(
            String queueToken,
            String userId
    );

    void leave(
            String queueToken,
            String userId
    );

    boolean canEnter(
            Long scheduleId,
            String queueToken,
            String userId
    );
}