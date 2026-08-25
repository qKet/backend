package com.exam.review.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.util.WebUtil;
import com.exam.reservation.dto.ReservationDTO;
import com.exam.review.dto.ReviewDTO;
import com.exam.review.dto.ReviewRequest;
import com.exam.review.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /***********************************
     *  URL      :  "/events/{performanceId}/reviews"
     *  이름      :   감상평 목록 조회
     *  기능      :   해당 공연의 감상평 목록을 조회한다 (공개, 로그인 불필요)
     *  method   :   GET
     ************************************/
    @GetMapping("/events/{performanceId}/reviews")
    public List<ReviewDTO> list(@PathVariable Long performanceId) {
        return reviewService.list(performanceId);
    }

    /***********************************
     *  URL      :  "/events/{performanceId}/reviews/rounds"
     *  이름      :   감상평 작성 가능 회차 조회
     *  기능      :   로그인한 사용자가 이 공연에서 예매한 회차 목록을 조회한다 (작성 화면의 회차 선택용)
     *  method   :   GET
     ************************************/
    @GetMapping("/events/{performanceId}/reviews/rounds")
    public List<ReservationDTO> reviewableRounds(@PathVariable Long performanceId, HttpSession session) {
        UserDTO loginUser = requireLogin(session);
        return reviewService.reviewableRounds(performanceId, loginUser.getUserId());
    }

    /***********************************
     *  URL      :  "/events/{performanceId}/reviews"
     *  이름      :   감상평 작성
     *  기능      :   해당 회차를 예매한 사용자가 감상평을 작성한다 (회차당 1개만 허용)
     *  method   :   POST
     ************************************/
    @PostMapping("/events/{performanceId}/reviews")
    public ReviewDTO write(@PathVariable Long performanceId,
                            @RequestBody ReviewRequest request,
                            HttpSession session,
                            HttpServletRequest servletRequest) {
        UserDTO loginUser = requireLogin(session);
        return reviewService.write(performanceId, request.getRoundId(), loginUser.getUserId(), request.getContent(),
                request.getRating(), WebUtil.getClientIp(servletRequest));
    }

    /***********************************
     *  URL      :  "/reviews/{reviewId}"
     *  이름      :   감상평 수정
     *  기능      :   본인이 작성한 감상평을 수정한다
     *  method   :   PUT
     ************************************/
    @PutMapping("/reviews/{reviewId}")
    public ReviewDTO update(@PathVariable Long reviewId,
                             @RequestBody ReviewRequest request,
                             HttpSession session,
                             HttpServletRequest servletRequest) {
        UserDTO loginUser = requireLogin(session);
        return reviewService.update(reviewId, loginUser.getUserId(), request.getContent(), request.getRating(),
                WebUtil.getClientIp(servletRequest));
    }

    /***********************************
     *  URL      :  "/reviews/{reviewId}"
     *  이름      :   감상평 삭제
     *  기능      :   본인이 작성한 감상평을 삭제한다 (소프트 삭제)
     *  method   :   DELETE
     ************************************/
    @DeleteMapping("/reviews/{reviewId}")
    public void delete(@PathVariable Long reviewId,
                        HttpSession session,
                        HttpServletRequest servletRequest) {
        UserDTO loginUser = requireLogin(session);
        reviewService.delete(reviewId, loginUser.getUserId(), WebUtil.getClientIp(servletRequest));
    }

    private UserDTO requireLogin(HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return loginUser;
    }
}
