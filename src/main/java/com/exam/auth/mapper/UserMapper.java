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
}
