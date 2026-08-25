package com.exam.auth.service;

public interface EmailVerificationService {
    void sendCode(String email);
    void confirmCode(String email, String code);
}
