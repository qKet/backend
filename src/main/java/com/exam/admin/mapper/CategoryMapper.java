package com.exam.admin.mapper;

import com.exam.admin.dto.CategoryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {
    List<CategoryDTO> findActive();

    // 관리자 카테고리 관리 화면용 — 사용여부 상관없이 전체 조회
    List<CategoryDTO> findAll();
    boolean existsByName(@Param("categoryNm") String categoryNm);
    boolean existsByNameExcludingId(@Param("categoryNm") String categoryNm, @Param("categoryId") Long categoryId);
    int save(CategoryDTO categoryDTO);
    int updateCategory(CategoryDTO categoryDTO);

    boolean hasLinkedPerformances(@Param("categoryId") Long categoryId);
    int deleteCategory(@Param("categoryId") Long categoryId);
}
