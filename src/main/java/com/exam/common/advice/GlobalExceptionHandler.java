package com.exam.common.advice;

import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 컨트롤러가 못 잡고 흘린 예외를 여기 한 군데서 잡아서 ErrorResponse(status/code/message/timestamp) 로 통일.
// 성공 응답은 GlobalResponseAdvice, 실패 응답은 여기 — 서로 역할을 분리해서 상태코드/직렬화 충돌을 피함.
@Slf4j
@RestControllerAdvice(basePackages = "com.exam")
public class GlobalExceptionHandler {

    // 컨트롤러가 throw new BusinessException(ErrorCode.XXX) 로 던진 경우
    // e.getMessage()를 쓰는 이유: BusinessException(errorCode, "커스텀 메시지")로 던졌으면 그 메시지를,
    // BusinessException(errorCode)만 던졌으면 errorCode 기본 메시지를 그대로 씀 (둘 다 커버됨)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getErrorCode());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    // @Valid 붙은 DTO 검증 실패 시 (지금 당장 쓰는 곳은 없지만, 나중에 @Valid를 붙이면 바로 동작함)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus()).body(response);
    }

    // 그 외 예상 못한 서버 에러 — 원래는 스프링 기본 whitelabel 에러 페이지(HTML)가 나가서
    // 프론트 apiFetch의 JSON 파싱이 깨졌는데, 이제 일관된 ErrorResponse로 내려감
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
