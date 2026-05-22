package com.ecommerce.service;

import com.ecommerce.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();
    List<Category> getAdminCategories(Integer status);
    Category getCategoryById(Long id);
    Category createCategory(Category category);
    Category updateCategory(Long id, Category category);
    void deleteCategory(Long id);
}
