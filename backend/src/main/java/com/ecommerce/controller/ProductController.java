package com.ecommerce.controller;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.service.ProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<PageResult<ProductDTO>> getProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort) {
        return Result.success(productService.getProducts(page, size, categoryId, keyword, sort));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public Result<PageResult<ProductDTO>> getAllProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(productService.getAllProducts(page, size, categoryId, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<ProductDTO> getProductById(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public Result<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        return Result.success(productService.createProduct(productDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public Result<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        return Result.success(productService.updateProduct(id, productDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        productService.updateStatus(id, status);
        return Result.success();
    }

    @GetMapping("/top")
    public Result<List<ProductDTO>> getTopProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(productService.getTopProducts(limit));
    }
}