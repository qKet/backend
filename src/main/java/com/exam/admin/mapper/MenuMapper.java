package com.exam.admin.mapper;

import com.exam.admin.dto.MenuDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuMapper {
    List<MenuDTO> findAll();
    int save(MenuDTO menuDTO);
    int updateMenu(MenuDTO menuDTO);
    int deleteMenu(Long menuId);

    List<MenuDTO> findMyMenus(@Param("roleId") Long roleId);
}
