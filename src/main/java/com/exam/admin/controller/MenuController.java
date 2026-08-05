package com.exam.admin.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.admin.dto.MenuDTO;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.admin.service.MenuService;
import com.exam.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 메뉴관리(그리드) — 네비게이션 트리 구조 관리. 관리자(3)만
@RestController
@RequestMapping("/admin/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    private UserDTO getLoginUser(HttpSession session) {
        return (UserDTO) session.getAttribute("loginUser");
    }

    private boolean isAdmin(UserDTO user) {
        return user != null && Long.valueOf(3L).equals(user.getRoleId());
    }

    /***********************************
     * URL : "/admin/menus"
     * 이름 : 메뉴 목록 조회
     * 기능 : 등록된 메뉴 전체 목록 조회 (그리드)
     * method : Get
     ************************************/
    @GetMapping
    public List<MenuDTO> getMenus(HttpSession session) {
        if (!isAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        return menuService.getMenus();
    }

    /***********************************
     * URL : "/admin/menus"
     * 이름 : 메뉴 등록
     * 기능 : 새 메뉴(그리드 행) 추가
     * method : Post
     ************************************/
    @PostMapping
    public Map<String, Object> createMenu(@RequestBody MenuDTO body, HttpSession session,
            HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isAdmin(loginUser))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        body.setInsId(loginUser.getUserId());
        body.setInsIp(WebUtil.getClientIp(request));
        menuService.createMenu(body);
        return Map.of("success", true);
    }

    /***********************************
     * URL : "/admin/menus/{menuId}"
     * 이름 : 메뉴 수정
     * 기능 : 메뉴 이름/순서/부모/연결 프로그램/사용여부 수정
     * method : Put
     ************************************/
    @PutMapping("/{menuId}")
    public Map<String, Object> updateMenu(@PathVariable Long menuId, @RequestBody MenuDTO body,
            HttpSession session, HttpServletRequest request) {
        UserDTO loginUser = getLoginUser(session);
        if (!isAdmin(loginUser))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        body.setMenuId(menuId);
        body.setUptId(loginUser.getUserId());
        body.setUptIp(WebUtil.getClientIp(request));
        menuService.updateMenu(body);
        return Map.of("success", true);
    }

    /***********************************
     * URL : "/admin/menus/{menuId}"
     * 이름 : 메뉴 삭제
     * 기능 : 메뉴(그리드 행) 삭제
     * method : Delete
     ************************************/
    @DeleteMapping("/{menuId}")
    public Map<String, Object> deleteMenu(@PathVariable Long menuId, HttpSession session) {
        if (!isAdmin(getLoginUser(session)))
            throw new BusinessException(ErrorCode.ADMIN_ONLY);
        menuService.deleteMenu(menuId);
        return Map.of("success", true);
    }
}
