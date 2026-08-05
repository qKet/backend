package com.exam.admin.service;

import com.exam.admin.dto.CategoryDTO;
import com.exam.admin.mapper.CategoryMapper;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryDTO> getActiveCategories() {
        return categoryMapper.findActive();
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryMapper.findAll();
    }

    @Override
    public void createCategory(CategoryDTO categoryDTO) {
        if (categoryMapper.existsByName(categoryDTO.getCategoryNm()))
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 존재하는 카테고리명입니다.");
        categoryMapper.save(categoryDTO);
    }

    @Override
    public void updateCategory(CategoryDTO categoryDTO) {
        if (categoryDTO.getCategoryNm() != null
                && categoryMapper.existsByNameExcludingId(categoryDTO.getCategoryNm(), categoryDTO.getCategoryId()))
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 존재하는 카테고리명입니다.");
        // 미사용으로 바꾸면 공개 목록(getActiveCategories)에서 빠져서 홈 화면 필터/공연 등록 폼에서
        // 안 보이게 되는데, 이미 그 카테고리로 등록된 공연이 있으면 그 공연들만 고아처럼 남으므로 제한
        if ("N".equals(categoryDTO.getUseYn()) && categoryMapper.hasLinkedPerformances(categoryDTO.getCategoryId()))
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "연결된 공연이 있어 미사용으로 변경할 수 없습니다.");
        categoryMapper.updateCategory(categoryDTO);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        if (categoryMapper.hasLinkedPerformances(categoryId))
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "연결된 공연이 있어 삭제할 수 없습니다.");
        categoryMapper.deleteCategory(categoryId);
    }
}
