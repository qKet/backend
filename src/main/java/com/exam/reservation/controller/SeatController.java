package com.exam.reservation.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.queue.service.QueueService;
import com.exam.reservation.dto.SeatDTO;
import com.exam.reservation.service.SeatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/schedules")
public class SeatController {

    private final SeatService seatService;
    private final QueueService queueService;

    public SeatController(SeatService seatService, QueueService queueService) {
        this.seatService = seatService;
        this.queueService = queueService;
    }

    /***********************************
     *  URL      :   "/{scheduleId}/seats"
     *  이름      :   공연 좌석 조회
     *  기능      :   선택한 공연 회차의 좌석 목록을 조회. 로그인 + 대기열 통과 여부(canEnter)를
     *              검증 — 없으면 대기열 팝업 없이 URL 직접 접근으로 동시접속 제한을 우회할 수 있었음
     *  method   :   Get
     *  param    :   Long scheduleId, String queueToken, HttpSession
     *  return   :   List<SeatDTO>
     ************************************/
    @GetMapping("/{scheduleId}/seats")
    public List<SeatDTO> byRound(
            @PathVariable("scheduleId") Long roundId,
            @RequestParam(value = "queueToken", required = false) String queueToken,
            HttpSession session
    ) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");

        if (loginUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        if (queueToken == null || queueToken.isBlank()
                || !queueService.canEnter(roundId, queueToken, loginUser.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "대기열을 통해 입장해주세요."
            );
        }

        return seatService.getSeatsByRound(roundId);
    }
}
