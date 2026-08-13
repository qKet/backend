package com.exam.auth.service;

import com.exam.auth.dto.UserDTO;

public interface UserService {
    UserDTO login(String userId, String pwd);
    int register(UserDTO userDTO);
    // 이미 사용 중이면 BusinessException(USER_ID_ALREADY_EXISTS)을 던짐 — 통과하면 사용 가능
    void checkUserIdAvailable(String userId);
}
