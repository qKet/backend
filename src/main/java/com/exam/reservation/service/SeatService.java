package com.exam.reservation.service;

import com.exam.reservation.dto.SeatDTO;

import java.util.List;

public interface SeatService {
    List<SeatDTO> getSeatsByRound(Long roundId);
    SeatDTO getSeat(Long seatId);
}
