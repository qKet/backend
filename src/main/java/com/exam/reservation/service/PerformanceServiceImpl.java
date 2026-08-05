package com.exam.reservation.service;

import com.exam.common.dto.PageResponse;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.reservation.dto.PerformanceDTO;
import com.exam.reservation.dto.RoundDTO;
import com.exam.reservation.mapper.PerformanceMapper;
import org.springframework.stereotype.Service;


import java.util.List;
/**
 *
 파일명: PerformanceServiceImpl.java
 *
 **/
@Service
public class PerformanceServiceImpl implements PerformanceService {

    private final PerformanceMapper performanceMapper;

    public PerformanceServiceImpl(PerformanceMapper performanceMapper) {
        this.performanceMapper = performanceMapper;
    }
    /***********************************
     *  이름      :   getAllPerformances
     *  기능      :   공연 목록 조회, categoryId로 카테고리 필터링·keyword로 제목/공연장 검색 가능
     *  param    :   categoryId(선택, null이면 전체), keyword(선택, null/빈 문자열이면 전체)
     *  return   :   List<PerformanceDTO>
     ************************************/
    @Override
    public List<PerformanceDTO> getAllPerformances(Long categoryId, String keyword) {
        return performanceMapper.findAll(categoryId, keyword);
    }

    /***********************************
     *  이름      :   getPerformanceDetail
     *  기능      :   공연 상세 조회 - 공연정보 + 회차 + 캐스팅
     *  param    :
     *  return   :
     *  URL      :   "/events/{performanceId}"
     ************************************/

    @Override
    public PerformanceDTO getPerformanceDetail(Long performanceId) {
        PerformanceDTO performance = performanceMapper.findById(performanceId);
        if (performance == null){
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "존재하지 않는 공연입니다.");
        }
        performance.setCasts(performanceMapper.findCastsByPerformanceId(performanceId));
        return performance;
    }


    /***********************************
     *  이름      :   getRoundsByMonth
     *  기능      :   달력 화면에서 보고있는 달의 회차만 출력
     *  param    :
     *  return   :
     *  URL      :   "/events/{performanceId}/calender"
     ************************************/
    @Override
    public List<RoundDTO> getRoundsByMonth(Long performanceId, String month) {
        return performanceMapper.findRoundsByMonth(performanceId, month);
    }



    /***********************************
     *  이름      :   getPerformances
     *  기능      :   공연 목록 페이지 단위 조회 (메인 화면 페이지네이션용),
     *              categoryId로 카테고리 필터링·keyword로 제목/공연장 검색 가능
     *  param    :   page(1부터 시작), size, categoryId(선택), keyword(선택)
     *  return   :   PageResponse<PerformanceDTO>
     ************************************/

    @Override
    public PageResponse<PerformanceDTO> getPerformances(int page, int size, Long categoryId, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;
        List<PerformanceDTO> content = performanceMapper.findAllPaged(offset, safeSize, categoryId, keyword);
        long totalCount = performanceMapper.countAll(categoryId, keyword);
        return new PageResponse<>(content, safePage, safeSize, totalCount);
    }

//    @Override
//    public PerformanceDTO getPerformance(Long performanceId) {
//        return performanceMapper.findById(performanceId);
//    }

//    @Override
//    public List<PerformanceRoundDTO> getRounds(Long performanceId) {
//        return performanceMapper.findRoundsByPerformanceId(performanceId);
//    }

//    @Override
//    public PerformanceRoundDTO getRound(Long roundId) {
//        return performanceMapper.findRoundById(roundId);
//    }
}
