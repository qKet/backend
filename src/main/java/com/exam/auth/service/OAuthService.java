package com.exam.auth.service;

import com.exam.auth.dto.UserDTO;
import com.exam.auth.oauth.OAuthUserInfo;
import jakarta.servlet.http.HttpServletRequest;

public interface OAuthService {
    UserDTO loginOrRegister(OAuthUserInfo info, HttpServletRequest request);
}
