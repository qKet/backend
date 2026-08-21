package com.exam.auth.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

// 비밀번호 재설정 링크의 토큰 저장소(MEM02_LOGIN01). EmailVerificationRepository와 동일하게
// StringRedisTemplate + TTL 방식 — 토큰 자체가 키, 값은 userId(있으면 유효, 없으면 만료/이미 사용됨).
@Repository
public class PasswordResetRepository {

    private final StringRedisTemplate redisTemplate;

    public PasswordResetRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveToken(String token, String userId, Duration ttl) {
        redisTemplate.opsForValue().set(tokenKey(token), userId, ttl);
    }

    public String findUserId(String token) {
        return redisTemplate.opsForValue().get(tokenKey(token));
    }

    // 1회용 토큰 — 사용 직후 반드시 지워서 같은 링크로 재요청 못 하게 함
    public void deleteToken(String token) {
        redisTemplate.delete(tokenKey(token));
    }

    private String tokenKey(String token) {
        return "pwd-reset:token:" + token;
    }
}
