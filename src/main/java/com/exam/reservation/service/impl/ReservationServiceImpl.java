package com.exam.reservation.service.impl;

import com.exam.notification.service.ReservationNotificationService;
import com.exam.queue.service.QueueService;
import com.exam.reservation.dto.ReservationDTO;
import com.exam.reservation.mapper.ReservationMapper;
import com.exam.reservation.service.ReservationService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/**
 *
 파일명: ReservationServiceImpl.java
 *
 **/
@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final RedissonClient redissonClient;
    private final QueueService queueService;
    private final ReservationNotificationService reservationNotificationService;

    public ReservationServiceImpl(ReservationMapper reservationMapper,
                                  RedissonClient redissonClient,
                                  QueueService queueService,
                                  ReservationNotificationService reservationNotificationService) {
        this.reservationMapper = reservationMapper;
        this.redissonClient = redissonClient;
        this.queueService = queueService;
        this.reservationNotificationService = reservationNotificationService;
    }

    @Override
    @Transactional
    /***********************************
     *  이름      :   reserve
     *  기능      :   공연 좌석 예매 (동시성 처리) — HOLD01_HOLD03: 락 구현을 직접 짠 SETNX+Lua에서
     *              Redisson RLock으로 교체. 동작(동일 좌석 동시요청 중 1건만 성공)은 기존과 동일
     *  param    :  String,Long,Long,Long,String
     *  return   :   Map<String, Object>
     ************************************/
    public Map<String, Object> reserve(String userId, Long reservationId, Long roundId, Long seatId, String queueToken, String clientIp) {
        String lockKey = "lock:reservation:" + seatId;
        RLock lock = redissonClient.getLock(lockKey);

        // waitTime=0: 기존 SETNX와 동일하게 "선점 실패 시 대기하지 않고 즉시 실패" 유지.
        // leaseTime=10초: 기존 TTL(10초)과 동일 — 이 값을 주면 Redisson의 자동 연장(watchdog)이 꺼지고
        // 정확히 10초 뒤 자동 해제됨. (watchdog을 쓰려면 leaseTime을 아예 생략해야 하는데,
        // "기존로직 유지" 요구사항이라 TTL을 그대로 고정값 10초로 맞춤 — 코드 리뷰 참고)
        boolean acquired;
        try {
            acquired = lock.tryLock(0, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("success", false, "message", "예매 처리 중 오류가 발생했습니다.");
        }
        if (!acquired) {
            return Map.of("success", false, "message", "이미 다른 사용자가 예매 중인 좌석입니다.");
        }

        try {
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

            reservationNotificationService.notifyConfirmed(userId, seatId, roundId);
            return Map.of("success", true, "message", "예매가 완료되었습니다.");
        } finally {
            // isHeldByCurrentThread()로 먼저 확인하는 이유: TTL(10초)이 이미 만료돼서 다른 스레드가
            // 새로 락을 잡은 상태에서 이 스레드가 뒤늦게 unlock()을 호출하면 IllegalMonitorStateException이
            // 나거나(Redisson이 소유자 아님을 감지) 최악의 경우 남의 락을 풀어버릴 수 있음 —
            // 기존 compare-and-delete Lua 스크립트가 하던 일을 Redisson이 이 체크로 대신해줌
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    /***********************************
     *  이름      :  getMyReservations
     *  기능      :  내 예매 내역 조회 기능
     *  param    :  String
     *  return   :  List<ReservationDTO>
     ************************************/
    @Override
    public List<ReservationDTO> getMyReservations(String userId) {
        return reservationMapper.findByUserId(userId);
    }

    /***********************************
     *  이름      :  getHistoryForAdmin
     *  기능      :  관리자 "예매 활동 로그" 보고서 — 기간(필수) + 사용자/액션(선택) 필터로
     *              RESERVATION_HISTORY 조회. 권한 체크는 AdminReservationController가 전담.
     *  param    :  String,String,String,String
     *  return   :  List<ReservationDTO>
     ************************************/
    @Override
    public List<ReservationDTO> getHistoryForAdmin(String from, String to, String userId, String action) {
        return reservationMapper.findHistoryForAdmin(from, to, userId, action);
    }

    @Override
    @Transactional
    /***********************************
     *  이름      :  cancel
     *  기능      :  예매 취소 기능
     *  param    :  Long,String
     *  return   :  Map<String, Object>
     ************************************/
    public Map<String, Object> cancel(Long reservationId, String userId, String clientIp) {
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

        // 취소한 본인에게 취소 확인 메일
        reservationNotificationService.notifyCancelled(userId, reservation.getSeatId(), reservation.getRoundId());

        return Map.of("success", true, "message", "예매가 취소되었습니다.");
    }
}