package com.exam.admin.service;

import com.exam.admin.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {
    List<CategoryDTO> getActiveCategories();

    List<CategoryDTO> getAllCategories();
    void createCategory(CategoryDTO categoryDTO);
    void updateCategory(CategoryDTO categoryDTO);
    void deleteCategory(Long categoryId);
}
