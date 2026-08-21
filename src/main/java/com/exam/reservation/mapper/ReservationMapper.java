package com.exam.reservation.mapper;

import com.exam.reservation.dto.ReservationDTO;
import com.exam.reservation.dto.SeatDisplayInfoDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReservationMapper {
    int save(ReservationDTO reservationDTO);
    int insertHistory(ReservationDTO reservationDTO);
    List<ReservationDTO> findByUserId(String userId);
    List<ReservationDTO> findHistoryForAdmin(@Param("from") String from, @Param("to") String to,
                                              @Param("userId") String userId, @Param("action") String action);
    ReservationDTO findById(Long reservationId);
    int cancel(@Param("reservationId") Long reservationId, @Param("uptId") String uptId, @Param("uptIp") String uptIp);
    SeatDisplayInfoDTO findSeatDisplayInfo(@Param("seatId") Long seatId, @Param("roundId") Long roundId);
    // 감상평(REV01) 작성 자격 체크용 — 해당 회차를 실제로 예매했는지 여부
    int countReservationByRound(@Param("userId") String userId, @Param("roundId") Long roundId);
    // 감상평 작성 화면의 회차 선택 드롭다운용 — 이 공연에서 사용자가 예매한 회차 목록
    List<ReservationDTO> findReservedRoundsByPerformance(@Param("userId") String userId, @Param("performanceId") Long performanceId);
}
