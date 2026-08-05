package com.exam.admin.service;

import com.exam.admin.dto.MenuDTO;
import com.exam.admin.dto.MenuTreeDTO;

import java.util.List;

public interface MenuService {
    List<MenuDTO> getMenus();
    void createMenu(MenuDTO menuDTO);
    void updateMenu(MenuDTO menuDTO);
    void deleteMenu(Long menuId);

    List<MenuTreeDTO> getMyMenuTree(Long roleId);
}
