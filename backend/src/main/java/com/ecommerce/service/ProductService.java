package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.ProductDTO;

import java.util.List;

public interface ProductService {
    PageResult<ProductDTO> getProducts(Integer page, Integer size, Long categoryId, String keyword, String sort);
    PageResult<ProductDTO> getAllProducts(Integer page, Integer size, Long categoryId, String keyword, Integer status);
    ProductDTO getProductById(Long id);
    ProductDTO createProduct(ProductDTO productDTO);
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    void deleteProduct(Long id);
    void updateStatus(Long id, Integer status);
    List<ProductDTO> getTopProducts(Integer limit);
}