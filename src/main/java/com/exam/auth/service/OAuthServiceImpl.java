package com.exam.auth.service;

import com.exam.auth.dto.UserDTO;
import com.exam.auth.mapper.UserMapper;
import com.exam.auth.oauth.OAuthUserInfo;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class OAuthServiceImpl implements OAuthService {

    // USERS.user_id 는 VARCHAR(50) PK
    private static final int MAX_USER_ID_LENGTH = 50;
    // 카카오 등 이메일 동의항목이 안 열린 provider는 email이 안 내려와서, 로그인 자체가 막히지 않도록 임시 이메일을 만들어 씀
    private static final String FALLBACK_EMAIL_DOMAIN = "qket.local";

    private final UserMapper userMapper;

    public OAuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserDTO loginOrRegister(OAuthUserInfo info, HttpServletRequest request) {
        UserDTO existing = userMapper.findByProviderAndProviderUserId(info.provider(), info.providerUserId());
        if (existing != null) {
            existing.setPwd(null);
            return existing;
        }

        String email = (info.email() != null && !info.email().isBlank())
                ? info.email()
                : info.provider() + "_" + info.providerUserId() + "@" + FALLBACK_EMAIL_DOMAIN;

        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        UserDTO newUser = new UserDTO();
        newUser.setUserId(generateUserId(info.provider(), info.providerUserId()));
        newUser.setUserNm(info.name() != null && !info.name().isBlank() ? info.name() : info.provider() + " 사용자");
        newUser.setUserEmail(email);
        newUser.setPwd(null);
        newUser.setLoginProvider(info.provider());
        newUser.setProviderUserId(info.providerUserId());
        newUser.setInsId("SYSTEM");
        newUser.setInsIp(WebUtil.getClientIp(request));

        userMapper.save(newUser);
        // insert 문에서 role_id/user_status를 DB 쪽이 채우므로(하드코딩된 기본값), 세션에 온전한 상태를 담기 위해 재조회
        return userMapper.findByProviderAndProviderUserId(info.provider(), info.providerUserId());
    }

    // provider+providerUserId가 50자를 넘으면(예: Naver의 해시형 id) 짧은 해시로 대체해 PK 길이를 보장
    private String generateUserId(String provider, String providerUserId) {
        String candidate = provider + "_" + providerUserId;
        if (candidate.length() <= MAX_USER_ID_LENGTH) {
            return candidate;
        }
        return provider + "_" + sha256Hex(providerUserId).substring(0, 32);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
