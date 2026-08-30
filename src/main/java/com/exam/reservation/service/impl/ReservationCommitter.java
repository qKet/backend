package com.exam.reservation.service.impl;

import com.exam.queue.service.QueueService;
import com.exam.reservation.dto.ReservationDTO;
import com.exam.reservation.mapper.ReservationMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// ReservationServiceImpl.reserve()/cancel()에서 "DB에 실제로 쓰는 부분"만 떼어낸 트랜잭션 경계.
// 별도 빈으로 분리한 이유는 PaymentReservationCommitter와 동일(self-invocation으로
// @Transactional이 무시되는 문제 방지).
//
// 예매/취소 확정 알림(SQS 통신 포함)은 여기 안 넣고 호출부에서 트랜잭션·분산락이 끝난 다음에
// 실행함 — DB 커넥션과 좌석 락을 붙잡은 채로 느린 외부 통신을 안 하기 위함.
@Component
class ReservationCommitter {

    private final ReservationMapper reservationMapper;
    private final QueueService queueService;

    ReservationCommitter(ReservationMapper reservationMapper, QueueService queueService) {
        this.reservationMapper = reservationMapper;
        this.queueService = queueService;
    }

    @Transactional
    Map<String, Object> commitReserve(String userId, Long reservationId, Long roundId, Long seatId,
                                       String queueToken, String clientIp) {
        ReservationDTO reservation = new ReservationDTO();
        reservation.setUserId(userId);
        reservation.setReservationId(reservationId);
        reservation.setSeatId(seatId);
        reservation.setRoundId(roundId);
        // RESERVATIONS 는 UPDATE(누가 예매했는지), RESERVATION_HISTORY 는 INSERT(누가 이 이력을 남겼는지) — 행위자·IP는 둘 다 동일
        reservation.setUptId(userId);
        reservation.setUptIp(clientIp);
        reservation.setInsId(userId);
        reservation.setInsIp(clientIp);

        int affected = reservationMapper.save(reservation);
        if (affected == 0) {
            return Map.of("success", false, "message", "이미 예매된 좌석입니다.");
        }

        reservation.setAction("RESERVED");
        reservationMapper.insertHistory(reservation);

        // 예매 성공 시 대기열 active 자리 즉시 반납
        // 대기열을 거치지 않고 들어온 요청일 수도 있으니 토큰 없으면 그냥 건너뜀
        // 반납 자체가 실패해도 예매 성공에는 영향 주지 않도록 예외를 삼킴
        if (queueToken != null && !queueToken.isBlank()) {
            try {
                queueService.leave(queueToken, userId);
            } catch (Exception e) {
                // 반납 실패는 로그만 남기고 무시 (TTL로 나중에 자동 정리됨)
            }
        }

        return Map.of("success", true, "message", "예매가 완료되었습니다.");
    }

    @Transactional
    Map<String, Object> commitCancel(Long reservationId, String userId, String clientIp) {
        ReservationDTO reservation = reservationMapper.findById(reservationId);
        if (reservation == null || !reservation.getUserId().equals(userId)) {
            return Map.of("success", false, "message", "예매 정보를 찾을 수 없습니다.");
        }
        if (!"RESERVED".equals(reservation.getReservedStatus())) {
            return Map.of("success", false, "message", "이미 취소된 예매입니다.");
        }

        reservationMapper.cancel(reservationId, userId, clientIp);

        reservation.setAction("CANCELLED");
        reservation.setInsId(userId);
        reservation.setInsIp(clientIp);
        reservationMapper.insertHistory(reservation);

        // seatId/roundId는 여기서 알림 발송을 못 하니(트랜잭션 밖에서 해야 함) 호출부에 돌려줘서
        // ReservationServiceImpl.cancel()이 커밋 이후에 notifyCancelled를 부를 수 있게 함
        return Map.of(
                "success", true,
                "message", "예매가 취소되었습니다.",
                "seatId", reservation.getSeatId(),
                "roundId", reservation.getRoundId()
        );
    }
}
