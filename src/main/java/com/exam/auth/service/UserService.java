package com.exam.auth.service;

import com.exam.auth.dto.UserDTO;

public interface UserService {
    UserDTO login(String userId, String pwd);
    int register(UserDTO userDTO);
    // 이미 사용 중이면 BusinessException(USER_ID_ALREADY_EXISTS)을 던짐 — 통과하면 사용 가능
    void checkUserIdAvailable(String userId);
    // 비밀번호 재설정 링크 발급(MEM02_LOGIN01) — 아이디+이메일이 일치하는 LOCAL 계정에만 발송
    void requestPasswordResetCode(String userId, String userEmail);
    // 재설정 링크의 토큰 확인 후 새 비밀번호로 변경 (1회용 토큰)
    void resetPassword(String token, String newPwd, String clientIp);
}
