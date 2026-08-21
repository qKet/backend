package com.exam.auth.mapper;

import com.exam.auth.dto.UserDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    UserDTO findById(String userId);
    int save(UserDTO userDTO);
    List<UserDTO> findAll();
    int updateUser(UserDTO userDTO);
    List<Map<String, Object>> findAllRoles();
    UserDTO findByEmail(@Param("email") String email);
    UserDTO findByProviderAndProviderUserId(@Param("provider") String provider, @Param("providerUserId") String providerUserId);
    // 비밀번호 재설정(MEM02_LOGIN01) — 본인이 아니라 시스템이 대신 바꾸는 거라 uptId엔 userId 자신을 씀
    int updatePwd(@Param("userId") String userId, @Param("pwd") String pwd,
                  @Param("uptId") String uptId, @Param("uptIp") String uptIp);
}
