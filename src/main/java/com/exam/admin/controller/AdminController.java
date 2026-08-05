package com.exam.admin.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.auth.mapper.UserMapper;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 사용자/역할 관리 전용. 
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserMapper userMapper;

    public AdminController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    private UserDTO getLoginUser(HttpSession session) {
        return (UserDTO) session.getAttribute("loginUser");
    }

    private boolean isAdmin(UserDTO user) {
        return user != null && Long.valueOf(3L).equals(user.getRoleId());
    }

    /***********************************
     * URL : "/roles"
     * 이름 : 역할 목록 조회
     * 기능 : 역할 목록보기
     * method : Get
     ************************************/
    // 역할 목록 — 관리자(3)만
    @GetMapping("/roles")
    public List<Map<String, Object>> getRoles(HttpSession session) {
        if (!isAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        return userMapper.findAllRoles();
    }

    /***********************************
     * URL : "/users"
     * 이름 : 사용자 목록 조회
     * 기능 : 사용자 목록 조회하기
     * method : Get
     ************************************/
    // 사용자 목록 조회 — 관리자(3)만
    @GetMapping("/users")
    public List<UserDTO> getUsers(HttpSession session) {
        if (!isAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        return userMapper.findAll();
    }

    /***********************************
     * URL : "/users/{userId}"
     * 이름 : 사용자 정보 수정
     * 기능 : 관리자가 사용자의 상태 및 권한을 수정
     * method : Patch
     ************************************/
    // 사용자 상태/권한 수정 — 관리자(3)만
    @PatchMapping("/users/{userId}")
    public Map<String, Object> updateUser(@PathVariable String userId,
            @RequestBody UserDTO body,
            HttpSession session,
            HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isAdmin(loginUser))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        body.setUserId(userId);
        body.setUptId(loginUser.getUserId());
        body.setUptIp(WebUtil.getClientIp(request));
        userMapper.updateUser(body);
        return Map.of("success", true);
    }

    /***********************************
     * URL : "/users/batch"
     * 이름 : 사용자 정보 일괄 수정
     * 기능 : 관리자가 사용자 상태 및 권한 일괄 수정
     * method : Patch
     ************************************/
    // 사용자 상태/권한 일괄 수정 — 관리자(3)만
    @PatchMapping("/users/batch")
    public Map<String, Object> batchUpdateUsers(@RequestBody List<UserDTO> users, HttpSession session,
            HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isAdmin(loginUser))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        String clientIp = WebUtil.getClientIp(request);
        for (UserDTO user : users) {
            user.setUptId(loginUser.getUserId());
            user.setUptIp(clientIp);
            userMapper.updateUser(user);
        }
        return Map.of("success", true);
    }
}
