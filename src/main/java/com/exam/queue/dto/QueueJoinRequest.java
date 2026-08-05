package com.exam.queue.dto;

public record QueueJoinRequest(
        Long scheduleId
) {
}

//{
//  "scheduleId": 1  ==> 이런 요청 받을때 사용
//}