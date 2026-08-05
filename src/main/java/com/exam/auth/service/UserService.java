package com.exam.auth.service;

import com.exam.auth.dto.UserDTO;

public interface UserService {
    UserDTO login(String userId, String pwd);
    int register(UserDTO userDTO);
}
