package com.exam.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength 6 — 시연용으로 로그인 시 BCrypt 연산 부하를 낮춤(기본값 10 대비 라운드 1/16).
        // 기존에 strength 10으로 만들어진 계정 해시는 그대로 유효함 — BCrypt 해시 문자열 자체에
        // strength가 포함돼 있어서 matches()가 저장된 값 기준으로 검증하기 때문에 재해싱 불필요.
        return new BCryptPasswordEncoder(6);
    }
}
