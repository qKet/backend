package com.exam.reservation.mapper;

import com.exam.reservation.dto.PerformanceDTO;
import com.exam.reservation.dto.RoundDTO;
import com.exam.reservation.dto.VenueDTO;
import com.exam.reservation.dto.CastDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PerformanceMapper {
    List<PerformanceDTO> findAll(@Param("categoryId") Long categoryId, @Param("keyword") String keyword);
    List<PerformanceDTO> findAllPaged(@Param("offset") int offset, @Param("size") int size,
                                        @Param("categoryId") Long categoryId, @Param("keyword") String keyword);
    long countAll(@Param("categoryId") Long categoryId, @Param("keyword") String keyword);
    List<VenueDTO> findAllVenues();
    int insert(PerformanceDTO performanceDTO);
    int insertRound(RoundDTO roundDTO);
    int initReservationSlots(@Param("roundId") Long roundId, @Param("performanceId") Long performanceId,
                              @Param("insId") String insId, @Param("insIp") String insIp);

    boolean hasPassedRound(Long performanceId);
    boolean hasPassedRoundById(Long roundId);
    int updatePerformance(PerformanceDTO performanceDTO);
    int deleteReservationHistoryByPerformanceId(Long performanceId);
    int deleteReservationsByPerformanceId(Long performanceId);
    int deleteRoundsByPerformanceId(Long performanceId);
    int deletePerformance(Long performanceId);
    int deleteReservationHistoryByRoundId(Long roundId);
    int deleteReservationsByRoundId(Long roundId);
    int deleteRound(Long roundId);
    int updateRound(RoundDTO roundDTO);

    //공연 상세
    PerformanceDTO findById(Long performanceId);
    List<CastDTO> findCastsByPerformanceId(Long performanceId);

    // 달력용 (month는 2026-08 형식)
    List<RoundDTO> findRoundsByMonth(@Param("performanceId") Long performanceId, @Param("month") String month);
}
