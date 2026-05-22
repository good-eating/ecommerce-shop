package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.entity.Category;
import com.ecommerce.service.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<Category>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public Result<List<Category>> getAdminCategories(
            @RequestParam(required = false) Integer status) {
        return Result.success(categoryService.getAdminCategories(status));
    }

    @GetMapping("/{id}")
    public Result<Category> getCategoryById(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SALES')")
    public Result<Category> createCategory(@RequestBody Category category) {
        return Result.success(categoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SALES')")
    public Result<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return Result.success(categoryService.updateCategory(id, category));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SALES')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
