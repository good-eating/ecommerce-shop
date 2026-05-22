package com.ecommerce.service;

import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;

import java.util.List;

public interface CartService {
    CartDTO getCart(Long userId);
    void addToCart(Long userId, Long productId, Integer quantity);
    void updateCartItem(Long userId, Long cartItemId, Integer quantity);
    void removeCartItem(Long userId, Long cartItemId);
    void clearCart(Long userId);
    void mergeCartOnLogin(Long userId, List<CartItemDTO> sessionItems);
}