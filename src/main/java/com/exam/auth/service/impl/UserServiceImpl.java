package com.exam.auth.service.impl;

import com.exam.auth.dto.UserDTO;
import com.exam.auth.mapper.UserMapper;
import com.exam.auth.repository.EmailVerificationRepository;
import com.exam.auth.repository.PasswordResetRepository;
import com.exam.auth.service.UserService;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/***********************************
 * 파일명 : UserServiceImpl.java
 * 기능 : userId와 같은 데이터가 있는지 조회
 * param : String, String
 * result : UserDTO (유저정보)
 ************************************/

@Service
public class UserServiceImpl implements UserService {

    // 비밀번호 재설정 링크의 유효시간(MEM02_LOGIN01) — 짧게 잡아서 링크 탈취 위험을 줄임
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String notificationQueueUrl;
    // 소셜 로그인 완료 후 리다이렉트할 프론트엔드 origin — 비밀번호 재설정 링크를 만들 때도 재사용
    private final String baseUrl;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                            EmailVerificationRepository emailVerificationRepository,
                            PasswordResetRepository passwordResetRepository,
                            SqsClient sqsClient,
                            ObjectMapper objectMapper,
                            @Value("${cloud.aws.sqs.notification-queue-url:}") String notificationQueueUrl,
                            @Value("${app.base-url}") String baseUrl) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.notificationQueueUrl = notificationQueueUrl;
        this.baseUrl = baseUrl;
    }

    /***********************************
     * 이름 : login
     * 기능 : userId와 같은 데이터가 있는지 조회
     * param : String, String
     * result : UserDTO (유저정보)
     ************************************/
    @Override
    public UserDTO login(String userId, String pwd) {
        UserDTO user = userMapper.findById(userId);
        if (user == null)
            return null;
        if (!passwordEncoder.matches(pwd, user.getPwd()))
            return null;
        if ("SUSPENDED".equals(user.getUserStatus())) {
            throw new IllegalStateException("정지된 계정입니다. 고객센터에 문의하세요.");
        }
        user.setPwd(null);
        return user;
    }

    /***********************************
     * 이름 : register
     * 기능 : 유저정보 DB에 저장 후 처리한 행 갯수 반환
     * param : UserDTO
     * result : int
     ************************************/
    @Override
    @Transactional
    public int register(UserDTO userDTO) {
        checkUserIdAvailable(userDTO.getUserId());
        if (!emailVerificationRepository.isVerified(userDTO.getUserEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        userDTO.setPwd(passwordEncoder.encode(userDTO.getPwd())); //암호화
        int result = userMapper.save(userDTO); // INSERT, DELETE ,UPDATE 의 결과를 저장 시 처리한 행 갯수를 가져옴
        emailVerificationRepository.clearVerified(userDTO.getUserEmail()); // 재사용 방지
        return result;
    }

    /***********************************
     * 이름 : checkUserIdAvailable
     * 기능 : 아이디 중복확인 — 프론트의 "중복확인" 버튼 + register() 직전 방어적 재확인 둘 다 여기로 옴
     * param : String
     ************************************/
    @Override
    public void checkUserIdAvailable(String userId) {
        if (userMapper.findById(userId) != null) {
            throw new BusinessException(ErrorCode.USER_ID_ALREADY_EXISTS);
        }
    }

    /***********************************
     * 이름 : requestPasswordResetCode
     * 기능 : 아이디+이메일이 일치하는 LOCAL 계정에 비밀번호 재설정 링크를 발급해 이메일로 발송(MEM02_LOGIN01).
     *       발송 자체는 EmailVerificationServiceImpl과 동일하게 SQS에 넣기만 하고, 실제 이메일은
     *       notification-mailer Lambda가 type(PASSWORD_RESET)을 보고 처리한다.
     * param : String, String
     ************************************/
    @Override
    public void requestPasswordResetCode(String userId, String userEmail) {
        UserDTO user = userMapper.findById(userId);
        if (user == null || !user.getUserEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TARGET_NOT_FOUND);
        }
        if (user.getPwd() == null) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_NO_PASSWORD);
        }
        if (notificationQueueUrl.isBlank()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_SEND_FAILED);
        }

        // 추측 불가능한 랜덤 토큰 — 값은 userId, 토큰 자체가 Redis 키(있으면 유효, 없으면 만료/이미 사용됨)
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        passwordResetRepository.saveToken(token, userId, RESET_TOKEN_TTL);

        String link = baseUrl + "/find-password/confirm?token=" + token;
        try {
            String body = objectMapper.writeValueAsString(
                    Map.of("type", "PASSWORD_RESET", "email", userEmail, "link", link));
            sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(notificationQueueUrl).messageBody(body).build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_SEND_FAILED);
        }
    }

    /***********************************
     * 이름 : resetPassword
     * 기능 : 재설정 링크의 토큰 확인 후 새 비밀번호로 변경 (1회용 토큰, 사용 후 즉시 삭제)
     * param : String, String, String
     ************************************/
    @Override
    @Transactional
    public void resetPassword(String token, String newPwd, String clientIp) {
        String userId = passwordResetRepository.findUserId(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
        }
        passwordResetRepository.deleteToken(token);

        userMapper.updatePwd(userId, passwordEncoder.encode(newPwd), userId, clientIp);
    }
}
