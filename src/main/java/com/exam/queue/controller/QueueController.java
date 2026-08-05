package com.exam.queue.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.queue.dto.QueueJoinRequest;
import com.exam.queue.dto.QueueJoinResponse;
import com.exam.queue.dto.QueueStatusResponse;
import com.exam.queue.service.QueueService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/queues")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }
    /***********************************
     *  URL      :  "/queues"
     *  이름      :   join
     *  기능      :   대기열 참가
     *  method   :   Post
     *  param    :   QueueJoinRequest, HttpSession
     *  result   :   QueueJoinResponse
     ************************************/
    @PostMapping
    public QueueJoinResponse join(
            @RequestBody QueueJoinRequest request,
            HttpSession session
    ) {
        UserDTO loginUser = getLoginUser(session);

        if (request.scheduleId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "회차 ID가 필요합니다."
            );
        }

        return queueService.join(
                request.scheduleId(),
                String.valueOf(loginUser.getUserId())
        );
    }
    /***********************************
     *  URL      :  "/queues/{queueToken}
     *  이름      :   getStatus
     *  기능      :   대기열 조회
     *  method   :   Get
     *  param    :   QueueToken, HttpSession
     *  result   :   QueueStatusResponse
     ************************************/
    @GetMapping("/{queueToken}")
    public QueueStatusResponse getStatus(
            @PathVariable String queueToken,
            HttpSession session
    ) {
        UserDTO loginUser = getLoginUser(session);

        return queueService.getStatus(
                queueToken,
                String.valueOf(loginUser.getUserId())
        );
    }

    // 프론트 beforeunload 시 navigator.sendBeacon() 으로 호출하는 이탈 API
    // sendBeacon 은 브라우저 스펙상 POST 요청만 가능해서 DELETE 대신 POST + /leave 경로로 통일
    /***********************************
     *  URL      :  "/queues/{queueToken}/leave
     *  이름      :   leave
     *  기능      :   대기열 나가기
     *  method   :   Post
     *  param    :   QueueToken, HttpSession
     *  result   :   Map<String, Object>
     ************************************/
    @PostMapping("/{queueToken}/leave")
    public Map<String, Object> leave(
            @PathVariable String queueToken,
            HttpSession session
    ) {
        UserDTO loginUser = getLoginUser(session);

        queueService.leave(
                queueToken,
                String.valueOf(loginUser.getUserId())
        );

        return Map.of("success", true);
    }

    private UserDTO getLoginUser(HttpSession session) {
        UserDTO loginUser =
                (UserDTO) session.getAttribute("loginUser");

        if (loginUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return loginUser;
    }
}