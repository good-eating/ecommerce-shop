package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.security.UserDetailsImpl;
import com.ecommerce.service.CartService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Result<CartDTO> getCart(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return Result.success(cartService.getCart(userDetails.getId()));
    }

    @PostMapping("/items")
    public Result<Void> addToCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        cartService.addToCart(userDetails.getId(), productId, quantity);
        return Result.success();
    }

    @PutMapping("/items/{cartItemId}")
    public Result<Void> updateCartItem(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {
        cartService.updateCartItem(userDetails.getId(), cartItemId, quantity);
        return Result.success();
    }

    @DeleteMapping("/items/{cartItemId}")
    public Result<Void> removeCartItem(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long cartItemId) {
        cartService.removeCartItem(userDetails.getId(), cartItemId);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> clearCart(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        cartService.clearCart(userDetails.getId());
        return Result.success();
    }

    @PostMapping("/merge")
    public Result<Void> mergeCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody List<CartItemDTO> sessionItems) {
        cartService.mergeCartOnLogin(userDetails.getId(), sessionItems);
        return Result.success();
    }
}