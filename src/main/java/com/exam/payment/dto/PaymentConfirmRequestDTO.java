package com.exam.payment.dto;

import lombok.Data;

// POST /payments/confirm 요청 바디 — 결제 위젯(successUrl)에서 넘어온 토스 파라미터 +
// 좌석 선택 단계부터 쭉 쿼리스트링으로 들고 온 예매 슬롯 식별자
@Data
public class PaymentConfirmRequestDTO {

    private String paymentKey;
    private String orderId;
    private Long amount;

    private Long reservationId;
    private Long roundId;
    private Long seatId;
    private String queueToken;
}
