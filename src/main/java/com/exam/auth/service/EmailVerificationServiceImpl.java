package com.exam.auth.service;

import com.exam.auth.mapper.UserMapper;
import com.exam.auth.repository.EmailVerificationRepository;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    // 완벽한 RFC 검증은 아니고 "@ 앞뒤로 뭔가 있고 도메인에 . 이 있다" 수준의 최소 형식 체크 —
    // 존재하지 않는 메일함까지는 어차피 발송 전에 확인 불가(이메일의 근본적 한계)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final SecureRandom random = new SecureRandom();

    private final EmailVerificationRepository repository;
    private final UserMapper userMapper;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public EmailVerificationServiceImpl(EmailVerificationRepository repository,
                                         UserMapper userMapper,
                                         SqsClient sqsClient,
                                         ObjectMapper objectMapper,
                                         @Value("${cloud.aws.sqs.notification-queue-url:}") String queueUrl) {
        this.repository = repository;
        this.userMapper = userMapper;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public void sendCode(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이메일을 입력해주세요.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "올바른 이메일 형식이 아닙니다.");
        }
        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        if (queueUrl.isBlank()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_SEND_FAILED);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        repository.saveCode(email, code, CODE_TTL);

        try {
            String body = objectMapper.writeValueAsString(
                    Map.of("type", "EMAIL_VERIFICATION", "email", email, "code", code));
            sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(body).build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_SEND_FAILED);
        }
    }

    @Override
    public void confirmCode(String email, String code) {
        String saved = repository.findCode(email);
        if (saved == null) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        }
        if (!saved.equals(code)) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_MISMATCH);
        }
        repository.deleteCode(email);
        repository.markVerified(email, VERIFIED_TTL);
    }
}
