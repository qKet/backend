package com.exam.admin.service;

import com.exam.admin.dto.MenuDTO;
import com.exam.admin.dto.MenuTreeDTO;
import com.exam.admin.mapper.MenuMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;

    public MenuServiceImpl(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Override
    public List<MenuDTO> getMenus() {
        return menuMapper.findAll();
    }

    @Override
    public void createMenu(MenuDTO menuDTO) {
        menuMapper.save(menuDTO);
    }

    @Override
    public void updateMenu(MenuDTO menuDTO) {
        menuMapper.updateMenu(menuDTO);
    }

    @Override
    public void deleteMenu(Long menuId) {
        menuMapper.deleteMenu(menuId);
    }

    @Override
    public List<MenuTreeDTO> getMyMenuTree(Long roleId) {
        List<MenuDTO> flat = menuMapper.findMyMenus(roleId);

        Map<Long, MenuTreeDTO> byId = new LinkedHashMap<>();
        for (MenuDTO m : flat) {
            MenuTreeDTO node = new MenuTreeDTO();
            node.setMenuId(m.getMenuId());
            node.setMenuNm(m.getMenuNm());
            node.setUrlPath(m.getUrlPath());
            node.setSortOrder(m.getSortOrder());
            node.setParentMenuId(m.getParentMenuId());
            byId.put(m.getMenuId(), node);
        }

        List<MenuTreeDTO> roots = new ArrayList<>();
        for (MenuTreeDTO node : byId.values()) {
            MenuTreeDTO parent = node.getParentMenuId() == null ? null : byId.get(node.getParentMenuId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node); // parentMenuId 없거나, 부모가 접근 불가한 메뉴면 최상위로 노출
            }
        }

        // urlPath가 없는 "그룹 전용" 메뉴(예: 상단 "관리자" 드롭다운)는 연결된 페이지가 없어서
        // 하위메뉴가 하나도 안 남으면 클릭할 것도, 펼칠 것도 없는 빈 항목이 되므로 제거함
        roots.removeIf(node -> !hasContent(node));
        return roots;
    }

    private boolean hasContent(MenuTreeDTO node) {
        node.getChildren().removeIf(child -> !hasContent(child));
        boolean isGroupOnly = node.getUrlPath() == null;
        return !isGroupOnly || !node.getChildren().isEmpty();
    }
}
