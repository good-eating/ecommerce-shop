package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserDTO;
import com.ecommerce.security.UserDetailsImpl;
import com.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return Result.success();
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return Result.success(authService.refreshToken(token));
        }
        return Result.error("无效的token");
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Result<UserDTO> getProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return Result.success(authService.getCurrentUser(userDetails.getId()));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Result<UserDTO> updateProfile(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                          @RequestBody UserDTO userDTO) {
        return Result.success(authService.updateProfile(userDetails.getId(), userDTO));
    }
}