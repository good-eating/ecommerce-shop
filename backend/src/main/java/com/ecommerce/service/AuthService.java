package com.ecommerce.service;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserDTO;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    void logout(String token);
    AuthResponse refreshToken(String token);
    UserDTO getCurrentUser(Long userId);
    UserDTO updateProfile(Long userId, UserDTO userDTO);
}