package com.exam.common.exception;

import lombok.Getter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;

// 실패 응답 표준 포맷: { status, code, message, errors, timestamp }
// GlobalExceptionHandler가 만들어서 내려줌 — 컨트롤러는 이 클래스를 몰라도 됨 (BusinessException만 던지면 됨)
@Getter
public class ErrorResponse {

    private final int status;
    private final String code;
    private final String message;
    private final List<FieldErrorDetail> errors;
    private final LocalDateTime timestamp;

    private ErrorResponse(ErrorCode errorCode, String message, List<FieldErrorDetail> errors) {
        this.status = errorCode.getStatus().value();
        this.code = errorCode.getCode();
        this.message = message;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode, errorCode.getMessage(), List.of());
    }

    // BusinessException(errorCode, customMessage)처럼 메시지만 다르게 쓰고 싶을 때
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode, message, List.of());
    }

    // @Valid DTO 검증 실패 시 필드별 이유까지 담아서 응답 (지금은 쓰는 DTO가 없지만, 나중에 @Valid를 붙이면 바로 동작함)
    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        List<FieldErrorDetail> details = bindingResult.getFieldErrors().stream()
                .map(FieldErrorDetail::of)
                .toList();
        return new ErrorResponse(errorCode, errorCode.getMessage(), details);
    }

    @Getter
    public static class FieldErrorDetail {
        private final String field;
        private final String value;
        private final String reason;

        private FieldErrorDetail(String field, String value, String reason) {
            this.field = field;
            this.value = value;
            this.reason = reason;
        }

        public static FieldErrorDetail of(FieldError fieldError) {
            Object rejected = fieldError.getRejectedValue();
            return new FieldErrorDetail(
                    fieldError.getField(),
                    rejected == null ? "" : rejected.toString(),
                    fieldError.getDefaultMessage()
            );
        }
    }
}
