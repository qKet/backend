package com.exam.payment.service.impl;

import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.payment.dto.PaymentConfirmRequestDTO;
import com.exam.payment.dto.PaymentDTO;
import com.exam.payment.mapper.PaymentMapper;
import com.exam.reservation.service.ReservationService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

// PaymentServiceImpl.confirm()에서 "토스 결제 승인 이후"의 DB 작업(좌석 확보 + 결제 저장)만
// 떼어낸 트랜잭션 경계. 별도 빈으로 분리한 이유: @Transactional은 프록시 기반이라 같은 클래스
// 안에서 self-invocation(this.commit(...))하면 조용히 무시됨 — PaymentServiceImpl이 이 빈을
// 주입받아 호출해야만 실제로 트랜잭션이 걸리고, 토스 API 호출은 DB 커넥션을 안 붙잡게 됨.
@Component
class PaymentReservationCommitter {

    private final ReservationService reservationService;
    private final PaymentMapper paymentMapper;

    PaymentReservationCommitter(ReservationService reservationService, PaymentMapper paymentMapper) {
        this.reservationService = reservationService;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    PaymentDTO commit(PaymentConfirmRequestDTO request, String userId, String clientIp, Object payStatus) {
        Map<String, Object> reserveResult = reservationService.reserve(
                userId, request.getReservationId(), request.getRoundId(), request.getSeatId(),
                request.getQueueToken(), clientIp);

        if (!Boolean.TRUE.equals(reserveResult.get("success"))) {
            // 이 예외가 트랜잭션을 롤백시키고 PaymentServiceImpl.confirm()으로 전파됨 — 거기서
            // 토스 결제를 보상 취소(cancelWithToss). 여기서 직접 취소 API를 안 부르는 이유: 이
            // 메서드는 DB 트랜잭션 안이라, 외부 호출을 넣으면 원래 고치려던 문제가 재발함.
            throw new BusinessException(ErrorCode.SEAT_ALREADY_TAKEN);
        }

        PaymentDTO payment = new PaymentDTO();
        payment.setReservationId(request.getReservationId());
        payment.setUserId(userId);
        payment.setOrderId(request.getOrderId());
        payment.setPaymentKey(request.getPaymentKey());
        payment.setAmount(request.getAmount());
        payment.setPayStatus(String.valueOf(payStatus));
        payment.setApprovedAt(LocalDateTime.now());
        payment.setInsId(userId);
        payment.setInsIp(clientIp);

        try {
            paymentMapper.save(payment);
        } catch (DuplicateKeyException e) {
            // findByOrderId 체크 이후 극히 짧은 순간에 동일 orderId 요청이 동시에 들어와
            // 먼저 INSERT를 끝낸 경우 — 이번 요청은 실패 처리하지 않고 먼저 처리된 결과를 그대로 반환
            return paymentMapper.findByOrderId(request.getOrderId());
        }

        return payment;
    }
}
