package com.exam.auth.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.auth.service.UserService;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class UserController {

    UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    /***********************************
     *  URL      :  "/auth/login"
     *  이름      :   로그인
     *  기능      :   로그인 시킨다
     *  method   :   POST
     *  param    :   UserDTO, HttpSession
     *  result   :   Map<String, Object>
     *  Return   : ResponseEntity<Map<String, Object>>
     ************************************/
    // 실패 케이스(비번 틀림/정지 계정)는 Map을 손으로 만드는 대신 ErrorCode를 담아 BusinessException을 던짐
    // → GlobalExceptionHandler가 잡아서 { status, code, message, timestamp } 형태의 ErrorResponse로 응답
    // 성공 케이스는 그대로 Map 직접 리턴 (응답 모양을 안 바꾸기 위해 일부러 그대로 둠)
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody UserDTO userDTO, HttpSession session) {
        UserDTO user;
        try {
            user = userService.login(userDTO.getUserId(), userDTO.getPwd());
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.SUSPENDED_ACCOUNT);
        }
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        //spring session이 자동으로 user 세션정보 redis에 저장
        session.setAttribute("loginUser", user);
        return Map.of("success", true, "user", user);
    }

    /***********************************
     *  URL      :  "/auth/logout"
     *  이름      :   로그아웃
     *  기능      :   로그아웃 시킨다
     *  method   :   POST
     *  param    :   HttpSession
     *  result   :   Map<String, Object>
    ************************************/
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        //redis의 세션 제거
        session.invalidate();
        return Map.of("success", true);
    }

    /***********************************
     *  URL      :  "/auth/signup"
     *  이름      :   회원가입
     *  기능      :   회원가입 시킨다
     *  method   :   POST
     *  param    :   UserDTO
     *  result   :   Map<String, Object> 완료 메세지
     ************************************/
    @PostMapping("/signup")
    public Map<String, Object> register(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        // 회원가입은 로그인 전이라 행위자가 없음 → ins_id 는 고정값 'SYSTEM', ins_ip 는 실제 요청 IP
        userDTO.setInsId("SYSTEM");
        userDTO.setInsIp(WebUtil.getClientIp(request));
        try {
            userService.register(userDTO);
        } catch (BusinessException e) {
            // EMAIL_NOT_VERIFIED 등 — GlobalExceptionHandler가 처리하도록 그대로 던짐
            // (여기서 삼키면 프론트가 e.code로 분기 못 하고 항상 200+success:false만 받음)
            throw e;
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 사용 중인 아이디입니다.");
        }
        return Map.of("success", true, "message", "회원가입이 완료되었습니다.");
    }

    /***********************************
     *  URL      :  "/auth/me"
     *  이름      :   세션(redis) 확인
     *  기능      :   redis의 캐시를 통해 사용자가 로그인 상태인지 확인한다
     *  method   :   GET
     *  param    :   HttpSession
     *  result   :   Map<String, Object> 완료 메세지
     ************************************/
    @GetMapping("/me")
    public Map<String, Object> me(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        UserDTO user = (UserDTO) session.getAttribute("loginUser");
        if (user == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
        } else {
            result.put("success", true);
            result.put("user", user);
        }
        return result;
    }
}
