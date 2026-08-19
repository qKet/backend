package com.exam.admin.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.admin.dto.CategoryDTO;
import com.exam.admin.service.CategoryService;
import com.exam.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 카테고리관리 — 공연 카테고리(콘서트/뮤지컬 등) 등록/수정/삭제. 관리자(3)만
@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // 2026-08-18: 로그인/관리자(3) 여부 체크는 AdminAccessInterceptor가 미리 걸러줌.

    /***********************************
     * URL : "/admin/categories"
     * 이름 : 카테고리 목록 조회
     * 기능 : 등록된 공연 카테고리 전체 목록 조회 (관리 그리드, 사용여부 무관)
     * method : Get
     ************************************/
    @GetMapping
    public List<CategoryDTO> getCategories(HttpSession session) {
        return categoryService.getAllCategories();
    }

    /***********************************
     * URL : "/admin/categories"
     * 이름 : 카테고리 등록
     * 기능 : 관리자가 새 공연 카테고리를 등록 (카테고리명 중복 시 등록 제한)
     * method : Post
     ************************************/
    @PostMapping
    public Map<String, Object> createCategory(@RequestBody CategoryDTO body, HttpSession session,
            HttpServletRequest request) {
        UserDTO loginUser = WebUtil.getLoginUser(session);
        body.setInsId(loginUser.getUserId());
        body.setInsIp(WebUtil.getClientIp(request));
        categoryService.createCategory(body);
        return Map.of("success", true);
    }

    /***********************************
     * URL : "/admin/categories/{categoryId}"
     * 이름 : 카테고리 수정
     * 기능 : 관리자가 기존 등록된 공연 카테고리의 정보(이름/순서/사용여부)를 수정 (카테고리명 중복 시 수정 제한)
     * method : Put
     ************************************/
    @PutMapping("/{categoryId}")
    public Map<String, Object> updateCategory(@PathVariable Long categoryId, @RequestBody CategoryDTO body,
            HttpSession session, HttpServletRequest request) {
        UserDTO loginUser = WebUtil.getLoginUser(session);
        body.setCategoryId(categoryId);
        body.setUptId(loginUser.getUserId());
        body.setUptIp(WebUtil.getClientIp(request));
        categoryService.updateCategory(body);
        return Map.of("success", true);
    }

    /***********************************
     * URL : "/admin/categories/{categoryId}"
     * 이름 : 카테고리 삭제
     * 기능 : 관리자가 등록된 공연 카테고리를 삭제 (연결된 공연이 있으면 삭제 제한)
     * method : Delete
     ************************************/
    @DeleteMapping("/{categoryId}")
    public Map<String, Object> deleteCategory(@PathVariable Long categoryId, HttpSession session) {
        categoryService.deleteCategory(categoryId);
        return Map.of("success", true);
    }
}
