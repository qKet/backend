package com.exam.reservation.service.impl;

import com.exam.notification.service.ReservationNotificationService;
import com.exam.reservation.dto.ReservationDTO;
import com.exam.reservation.mapper.ReservationMapper;
import com.exam.reservation.service.ReservationService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

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
    private final ReservationCommitter reservationCommitter;
    private final ReservationNotificationService reservationNotificationService;

    public ReservationServiceImpl(ReservationMapper reservationMapper,
                                  RedissonClient redissonClient,
                                  ReservationCommitter reservationCommitter,
                                  ReservationNotificationService reservationNotificationService) {
        this.reservationMapper = reservationMapper;
        this.redissonClient = redissonClient;
        this.reservationCommitter = reservationCommitter;
        this.reservationNotificationService = reservationNotificationService;
    }

    @Override
    /***********************************
     *  이름      :   reserve
     *  기능      :   공연 좌석 예매(동시성 처리, Redisson RLock — 동일 좌석 동시요청 중 1건만 성공).
     *              DB 쓰기는 ReservationCommitter(별도 트랜잭션 빈)로 분리, 알림 발송(SQS)은
     *              트랜잭션·분산락이 모두 끝난 뒤로 옮김 — DB 커넥션/좌석 락을 오래 안 붙잡게
     *  param    :  String,Long,Long,Long,String
     *  return   :   Map<String, Object>
     ************************************/
    public Map<String, Object> reserve(String userId, Long reservationId, Long roundId, Long seatId, String queueToken, String clientIp) {
        String lockKey = "lock:reservation:" + seatId;
        RLock lock = redissonClient.getLock(lockKey);

        // waitTime=0: 선점 실패 시 대기 없이 즉시 실패. leaseTime=10초를 주면 Redisson 자동 연장
        // (watchdog)이 꺼지고 정확히 10초 뒤 자동 해제됨 — 고정 TTL을 원하므로 watchdog 미사용.
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

        Map<String, Object> result;
        try {
            result = reservationCommitter.commitReserve(userId, reservationId, roundId, seatId, queueToken, clientIp);
        } finally {
            // isHeldByCurrentThread() 확인 없이 unlock()하면, TTL 만료 후 다른 스레드가 이미 잡은
            // 락을 남이 풀어버릴 수 있음(IllegalMonitorStateException 또는 최악의 경우 오작동).
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        // 알림 발송은 DB 트랜잭션도 끝나고 분산락도 풀린 다음에 실행 — SQS 통신이 느려져도
        // 이 좌석을 노리는 다른 요청이나 DB 커넥션 풀에 영향을 안 주도록 함
        if (Boolean.TRUE.equals(result.get("success"))) {
            reservationNotificationService.notifyConfirmed(userId, seatId, roundId);
        }
        return result;
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
    /***********************************
     *  이름      :  cancel
     *  기능      :  예매 취소 — DB 쓰기는 ReservationCommitter로 분리, 취소 확인 알림(SQS)은
     *              트랜잭션이 끝난 뒤로 옮김(reserve()와 동일한 이유)
     *  param    :  Long,String
     *  return   :  Map<String, Object>
     ************************************/
    public Map<String, Object> cancel(Long reservationId, String userId, String clientIp) {
        Map<String, Object> result = reservationCommitter.commitCancel(reservationId, userId, clientIp);

        if (Boolean.TRUE.equals(result.get("success"))) {
            reservationNotificationService.notifyCancelled(
                    userId, (Long) result.get("seatId"), (Long) result.get("roundId"));
        }
        return result;
    }

    /***********************************
     *  이름      :  hasReservation
     *  기능      :  해당 사용자가 이 회차를 실제로 예매했는지 여부 (감상평 작성 자격 체크용)
     *  param    :  String, Long
     *  return   :  boolean
     ************************************/
    @Override
    public boolean hasReservation(String userId, Long roundId) {
        return reservationMapper.countReservationByRound(userId, roundId) > 0;
    }

    /***********************************
     *  이름      :  getReservedRounds
     *  기능      :  해당 공연에서 사용자가 예매한 회차 목록 (감상평 작성 시 회차 선택 드롭다운용)
     *  param    :  String, Long
     *  return   :  List<ReservationDTO>
     ************************************/
    @Override
    public List<ReservationDTO> getReservedRounds(String userId, Long performanceId) {
        return reservationMapper.findReservedRoundsByPerformance(userId, performanceId);
    }
}
