package com.ecommerce.service;

import com.ecommerce.dto.ProductDTO;

import java.util.List;

public interface RecommendationService {
    List<ProductDTO> getRecommendations(Long userId, Integer limit);
    List<ProductDTO> getPopularProducts(Integer limit);
    void computeItemSimilarity();
}