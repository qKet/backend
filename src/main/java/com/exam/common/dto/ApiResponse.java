package com.exam.common.dto;

import lombok.Getter;

import java.time.LocalDateTime;

// 모든 API 응답을 { success, message, data, timestamp } 형태로 통일하기 위한 공통 래퍼
// 컨트롤러가 직접 만드는 게 아니라 GlobalResponseAdvice 가 자동으로 감싸줌 — 컨트롤러는 이 클래스를 몰라도 됨
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
