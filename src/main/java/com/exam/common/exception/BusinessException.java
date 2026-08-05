package com.exam.common.exception;

import lombok.Getter;

// 컨트롤러가 실패 응답을 손으로 만드는 대신 throw new BusinessException(ErrorCode.XXX) 로 던지면
// GlobalExceptionHandler가 잡아서 ErrorResponse(status/code/message/timestamp)로 변환해줌
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // status/code는 errorCode 그대로 쓰되, 메시지만 상황에 맞게 바꾸고 싶을 때
    // (예: 같은 ROUND_ALREADY_OPEN 코드라도 "삭제할 수 없습니다"/"수정할 수 없습니다"처럼 문구는 다를 수 있음)
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
