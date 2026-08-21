package com.exam.reservation.service;

import com.exam.reservation.dto.ReservationDTO;

import java.util.List;
import java.util.Map;

public interface ReservationService {
    Map<String, Object> reserve(String userId, Long reservationId, Long roundId, Long seatId, String queueToken, String clientIp);
    List<ReservationDTO> getMyReservations(String userId);
    List<ReservationDTO> getHistoryForAdmin(String from, String to, String userId, String action);
    Map<String, Object> cancel(Long reservationId, String userId, String clientIp);
    // 감상평(REV01) 작성 자격 체크용 — 해당 회차를 실제로 예매했는지 여부
    boolean hasReservation(String userId, Long roundId);
    // 감상평 작성 화면의 회차 선택 드롭다운용 — 이 공연에서 사용자가 예매한 회차 목록
    List<ReservationDTO> getReservedRounds(String userId, Long performanceId);
}
