package com.exam.reservation.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.common.util.WebUtil;
import com.exam.queue.service.QueueService;
import com.exam.reservation.dto.ReservationDTO;
import com.exam.reservation.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final QueueService queueService;

    public ReservationController(ReservationService reservationService, QueueService queueService) {
        this.reservationService = reservationService;
        this.queueService = queueService;
    }

    /***********************************
     *  URL      :  "/reservations"
     *  이름      :   reserve
     *  기능      :   예약
     *
     *  2026-08-21: 대기열 통과 여부(canEnter) 검증을 추가함. 그전엔 queueToken이 예매 성공 후
     *  대기열 자리를 반납(leave)하는 용도로만 쓰이고 "이 토큰이 실제로 입장 자격이 있는지"는
     *  아무도 확인하지 않아서, 대기열 팝업을 거치지 않고 이 API를 직접 호출하면 동시 접속
     *  제한(MAX_ACTIVE_USERS=150)을 그대로 우회할 수 있었음.
     *
     *  검증을 ReservationServiceImpl.reserve()가 아니라 이 컨트롤러에 두는 이유:
     *  reserve()는 결제 승인(PaymentReservationCommitter.commit)에서도 호출되는데, 결제 흐름은
     *  토스 결제창에서 카드 입력/인증에 시간이 걸려 그 사이 대기열 자격(ACTIVE_TTL 10분)이
     *  만료될 수 있음. 서비스 레이어에서 막으면 "돈은 이미 결제됐는데 좌석 확정이 거부되고
     *  결제가 자동 취소되는" 상황을 새로 만들게 되므로, 직접 예약 경로에서만 검증함.
     *
     *  method   :   POST
     *  param    :   Map<String, Object>, HttpSession
     *  return   :   Map<String, Object>
     ************************************/
    @PostMapping
    public Map<String, Object> reserve(@RequestBody Map<String, Object> body, HttpSession session,
                                       HttpServletRequest request) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }
        Long seatId = toLong(body.get("seatId"));
        Long roundId = toLong(body.get("roundId"));
        Long reservationId = toLong(body.get("reservationId"));
        String queueToken = (String) body.get("queueToken");

        if (queueToken == null || queueToken.isBlank()
                || !queueService.canEnter(roundId, queueToken, loginUser.getUserId())) {
            return Map.of("success", false, "message", "대기열을 통해 입장해주세요. 대기 시간이 만료되었을 수 있습니다.");
        }

        return reservationService.reserve(loginUser.getUserId(), reservationId, roundId, seatId, queueToken,
                WebUtil.getClientIp(request));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        return Long.valueOf(value.toString());
    }

    /***********************************
     *  URL      :  "/reservations/my"
     *  이름      :   my
     *  기능      :   마이페이지 조회
     *  method   :   GET
     *  param    :   HttpSession
     *  return   :   Map<String, Object>
     ************************************/
    @GetMapping("/my")
    public Map<String, Object> myReservations(HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }
        List<ReservationDTO> list = reservationService.getMyReservations(loginUser.getUserId());
        return Map.of("success", true, "reservations", list);
    }

    /***********************************
     *  URL      :  "/reservations/{reservationId}"
     *  이름      :   cancel
     *  기능      :   예매 취소
     *  method   :   DELETE
     *  param    :   Long, HttpSession
     *  return   : Map<String, Object>
     ************************************/
    @DeleteMapping("/{reservationId}")
    public Map<String, Object> cancel(@PathVariable Long reservationId, HttpSession session,
                                      HttpServletRequest request) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }
        return reservationService.cancel(reservationId, loginUser.getUserId(), WebUtil.getClientIp(request));
    }
}
