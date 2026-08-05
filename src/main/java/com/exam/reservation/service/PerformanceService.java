package com.exam.reservation.service;
import com.exam.common.dto.PageResponse;
import com.exam.reservation.dto.PerformanceDTO;
import com.exam.reservation.dto.RoundDTO;


import java.util.List;

public interface PerformanceService {
    List<PerformanceDTO> getAllPerformances(Long categoryId, String keyword);
    PageResponse<PerformanceDTO> getPerformances(int page, int size, Long categoryId, String keyword);

    PerformanceDTO getPerformanceDetail(Long performanceId);
    List<RoundDTO> getRoundsByMonth(Long performanceId, String month);

//    List<PerformanceRoundDTO> getRounds(Long performanceId);
//    PerformanceRoundDTO getRound(Long roundId);
}
