package com.exam.reservation.mapper;

import com.exam.reservation.dto.SeatDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SeatMapper {
    List<SeatDTO> findByRoundId(Long roundId);
    SeatDTO findById(Long seatId);
    int updateStatus(Long seatId, String status);
}
