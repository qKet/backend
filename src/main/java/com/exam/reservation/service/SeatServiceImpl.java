package com.exam.reservation.service;
import com.exam.reservation.dto.SeatDTO;
import com.exam.reservation.mapper.SeatMapper;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 *
 파일명: SeatServiceImpl.java
 *
 **/
@Service
public class SeatServiceImpl implements SeatService {

    private final SeatMapper seatMapper;

    public SeatServiceImpl(SeatMapper seatMapper) {
        this.seatMapper = seatMapper;
    }
    /***********************************
     *  이름      :   getSeatsByRound
     *  기능      :   회차별 좌석 조회
     *  param    :   Long
     *  return   :   List<SeatDTO>
     ************************************/
    @Override
    public List<SeatDTO> getSeatsByRound(Long roundId) {
        return seatMapper.findByRoundId(roundId);
    }

    /***********************************
     *  이름      :   getSeat
     *  기능      :   좌석 상세 조회
     *  param    :   Long
     *  return   :   List<SeatDTO>
     ************************************/
    @Override
    public SeatDTO getSeat(Long seatId) {
        return seatMapper.findById(seatId);
    }
}
