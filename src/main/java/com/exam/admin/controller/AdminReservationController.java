package com.exam.admin.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.reservation.dto.ReservationDTO;
import com.exam.reservation.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 예매 활동 로그(보고서) — RESERVATION_HISTORY 기반 조회 전용. 관리자(3)만
@RestController
@RequestMapping("/admin/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    private UserDTO getLoginUser(HttpSession session) {
        return (UserDTO) session.getAttribute("loginUser");
    }

    private boolean isAdmin(UserDTO user) {
        return user != null && Long.valueOf(3L).equals(user.getRoleId());
    }

    /***********************************
     * URL : "/admin/reservations/history"
     * 이름 : 예매 활동 로그 조회
     * 기능 : 기간(필수) + 사용자/액션(선택) 필터로 RESERVATION_HISTORY 조회 —
     *       고객 문의 대응, 어뷰징(반복 예매/취소) 탐지, 운영 현황 파악용
     * method : Get
     ************************************/
    @GetMapping("/history")
    public List<ReservationDTO> getHistory(@RequestParam String from,
                                            @RequestParam String to,
                                            @RequestParam(required = false) String userId,
                                            @RequestParam(required = false) String action,
                                            HttpSession session) {
        if (!isAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        return reservationService.getHistoryForAdmin(from, to, userId, action);
    }
}
