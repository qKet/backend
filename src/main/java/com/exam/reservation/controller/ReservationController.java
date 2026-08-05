package com.exam.reservation.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.common.util.WebUtil;
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

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /***********************************
     *  URL      :  "/reservations"
     *  이름      :   reserve
     *  기능      :   예약
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
