package com.exam.reservation.controller;

import com.exam.reservation.dto.SeatDTO;
import com.exam.reservation.service.SeatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedules")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    /***********************************
     *  URL      :   "/{scheduleId}/seats"
     *  이름      :   공연 좌석 조회
     *  기능      :   선택한 공연 회차의 좌석 목록을 조회
     *  method   :   Get
     *  param    :   Long scheduleId
     *  return   :   List<SeatDTO>
     ************************************/
    @GetMapping("/{scheduleId}/seats")
    public List<SeatDTO> byRound(@PathVariable("scheduleId") Long roundId) {
        return seatService.getSeatsByRound(roundId);
    }
}
