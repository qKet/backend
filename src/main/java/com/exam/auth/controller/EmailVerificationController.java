package com.exam.auth.controller;

import com.exam.auth.service.EmailVerificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 회원가입 이메일 인증번호 발급/검증. 로그인 전 상태라 세션이 아니라 Redis(이메일 키)로 상태 관리
@RestController
@RequestMapping("/auth/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    /***********************************
     * URL : "/auth/email/verification-codes"
     * 이름 : 이메일 인증번호 발송
     * 기능 : 6자리 인증번호를 생성해 SQS로 발행(Lambda가 SES로 실제 발송), 5분 TTL로 Redis에 저장
     * method : Post
     ************************************/
    @PostMapping("/verification-codes")
    public Map<String, Object> send(@RequestBody Map<String, String> body) {
        emailVerificationService.sendCode(body.get("email"));
        return Map.of("success", true, "message", "인증번호를 발송했습니다.");
    }

    /***********************************
     * URL : "/auth/email/verification-codes/confirm"
     * 이름 : 이메일 인증번호 확인
     * 기능 : 입력한 인증번호를 저장된 값과 대조, 통과하면 30분간 "인증완료" 상태로 전환(회원가입 시 확인용)
     * method : Post
     ************************************/
    @PostMapping("/verification-codes/confirm")
    public Map<String, Object> confirm(@RequestBody Map<String, String> body) {
        emailVerificationService.confirmCode(body.get("email"), body.get("code"));
        return Map.of("success", true, "message", "이메일 인증이 완료되었습니다.");
    }
}
