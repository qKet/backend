package com.exam.auth.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

// 회원가입 이메일 인증번호 상태 저장소. RedisQueueRepository와 동일하게 StringRedisTemplate + TTL 방식.
// 코드 자체(code key)와 "검증 통과했음" 표시(verified key)를 분리해서, 검증 직후 code는 바로 지우고
// verified만 잠깐(가입 완료할 때까지) 남겨두는 구조 — 같은 코드를 재입력해서 다시 통과시키는 것 방지
@Repository
public class EmailVerificationRepository {

    private final StringRedisTemplate redisTemplate;

    public EmailVerificationRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveCode(String email, String code, Duration ttl) {
        redisTemplate.opsForValue().set(codeKey(email), code, ttl);
    }

    public String findCode(String email) {
        return redisTemplate.opsForValue().get(codeKey(email));
    }

    public void deleteCode(String email) {
        redisTemplate.delete(codeKey(email));
    }

    public void markVerified(String email, Duration ttl) {
        redisTemplate.opsForValue().set(verifiedKey(email), "true", ttl);
    }

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey(email)));
    }

    public void clearVerified(String email) {
        redisTemplate.delete(verifiedKey(email));
    }

    private String codeKey(String email) {
        return "email-verify:code:" + email;
    }

    private String verifiedKey(String email) {
        return "email-verify:verified:" + email;
    }
}
