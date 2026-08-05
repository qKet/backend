package com.exam.queue.domain;

public record QueueTokenInfo(
        Long scheduleId,
        String userId
) {
}