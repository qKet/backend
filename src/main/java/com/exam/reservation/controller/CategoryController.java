package com.exam.reservation.controller;

import com.exam.admin.dto.CategoryDTO;
import com.exam.admin.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 공연 카테고리 공개 조회 — 카테고리는 관리 대상 데이터라 admin 패키지(CategoryService)에 있지만,
// 홈 화면 카테고리 필터는 비로그인 사용자도 접근하는 기능이라 로그인이 필요한 CommonController가 아니라
// 여기서 노출한다 (CommonController가 admin.service.MenuService를 가져다 쓰는 것과 같은 취지의
// 의도적인 도메인 간 의존). 관리자용 카테고리 CRUD(/admin/categories)는 별도 컨트롤러로 추가될 예정
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /***********************************
     *  URL      :  "/categories"
     *  이름      :   카테고리 목록 조회
     *  기능      :   사용 중인 공연 카테고리 목록 조회 (홈 화면 카테고리 필터, 공연 등록/수정 폼의 카테고리 선택용)
     *  method   :   GET
     *  param    :
     *  result   :   List<CategoryDTO>
     ************************************/
    @GetMapping
    public List<CategoryDTO> list() {
        return categoryService.getActiveCategories();
    }
}
