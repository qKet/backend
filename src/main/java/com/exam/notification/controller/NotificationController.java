package com.exam.notification.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.util.WebUtil;
import com.exam.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

// NOTI01_ALERT01(공연 취소표 알림) — 회차 단위 구독 토글. 좌석 페이지의 "취소표 알림받기" 버튼이 호출
@RestController
@RequestMapping("/notifications/cancel-alerts")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private UserDTO requireLoginUser(HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return loginUser;
    }

    /***********************************
     *  URL      :   "/notifications/cancel-alerts/{roundId}"
     *  이름      :   구독 상태 조회
     *  기능      :   로그인 사용자가 해당 회차 취소표 알림을 구독 중인지 조회 — 좌석 페이지 진입 시 버튼 초기 상태용
     *  method   :   Get
     *  param    :   Long roundId, HttpSession
     *  return   :   boolean
     ************************************/
    @GetMapping("/{roundId}")
    public boolean isSubscribed(@PathVariable Long roundId, HttpSession session) {
        UserDTO loginUser = requireLoginUser(session);
        return notificationService.isSubscribed(loginUser.getUserId(), roundId);
    }

    /***********************************
     *  URL      :   "/notifications/cancel-alerts/{roundId}"
     *  이름      :   구독 켜기
     *  기능      :   해당 회차 취소표 알림 구독을 켬
     *  method   :   Post
     *  param    :   Long roundId, HttpSession, HttpServletRequest
     *  return   :   boolean
     ************************************/
    @PostMapping("/{roundId}")
    public boolean subscribe(@PathVariable Long roundId, HttpSession session, HttpServletRequest request) {
        UserDTO loginUser = requireLoginUser(session);
        notificationService.subscribe(loginUser.getUserId(), roundId, WebUtil.getClientIp(request));
        return true;
    }

    /***********************************
     *  URL      :   "/notifications/cancel-alerts/{roundId}"
     *  이름      :   구독 끄기
     *  기능      :   해당 회차 취소표 알림 구독을 끔
     *  method   :   Delete
     *  param    :   Long roundId, HttpSession, HttpServletRequest
     *  return   :   boolean
     ************************************/
    @DeleteMapping("/{roundId}")
    public boolean unsubscribe(@PathVariable Long roundId, HttpSession session, HttpServletRequest request) {
        UserDTO loginUser = requireLoginUser(session);
        notificationService.unsubscribe(loginUser.getUserId(), roundId, WebUtil.getClientIp(request));
        return false;
    }
}
