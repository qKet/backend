package com.exam.auth.service.impl;

import com.exam.auth.dto.UserDTO;
import com.exam.auth.mapper.UserMapper;
import com.exam.auth.repository.EmailVerificationRepository;
import com.exam.auth.service.UserService;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/***********************************
 * 파일명 : UserServiceImpl.java
 * 기능 : userId와 같은 데이터가 있는지 조회
 * param : String, String
 * result : UserDTO (유저정보)
 ************************************/

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                            EmailVerificationRepository emailVerificationRepository) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationRepository = emailVerificationRepository;
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
}
