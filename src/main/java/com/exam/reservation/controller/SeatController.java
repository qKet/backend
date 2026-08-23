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
     *  기능      :   선택한 공연 회차의 좌석 목록을 조회
     *
     *  2026-08-21: 로그인 + 대기열 통과 여부 검증을 추가함.
     *  그전엔 이 API에 아무 검사도 없어서, 대기열 팝업을 거치지 않고 브라우저 주소창에
     *  /seats/{roundId}를 직접 쳐도 좌석 화면이 그대로 열렸음 — MAX_ACTIVE_USERS(150)로
     *  동시 접속을 제한하려던 대기열 설계 전체가 프론트 UX에만 의존하고 있어서 사실상
     *  무력화된 상태였음(QueueServiceImpl.canEnter가 어디서도 호출되지 않고 있었음).
     *  좌석 화면에 도달할 수 있는 인원 자체가 대기열 상한(150명)으로 묶여 있으므로,
     *  3초 폴링마다 이 검사가 돌아도 Redis 부하는 초당 50건 수준이라 문제 없음.
     *
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
