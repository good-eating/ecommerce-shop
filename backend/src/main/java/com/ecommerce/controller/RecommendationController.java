package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.security.UserDetailsImpl;
import com.ecommerce.service.RecommendationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public Result<List<ProductDTO>> getRecommendations(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return Result.success(recommendationService.getRecommendations(userId, limit));
    }

    @GetMapping("/popular")
    public Result<List<ProductDTO>> getPopularProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(recommendationService.getPopularProducts(limit));
    }
}