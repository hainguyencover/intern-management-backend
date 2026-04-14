package com.holaho.intern.auth.controller;

import com.holaho.intern.shared.dto.request.ChangePasswordRequest;
import com.holaho.intern.shared.dto.request.LoginRequest;
import com.holaho.intern.shared.dto.request.RegisterRequest;
import com.holaho.intern.shared.dto.request.ResetPasswordRequest;
import com.holaho.intern.shared.dto.request.TokenRefreshRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.JwtResponse;
import com.holaho.intern.shared.dto.response.UserResponse;
import com.holaho.intern.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Auth Controller - Authentication & Authorization
 * Endpoints: /api/v1/auth/**
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Refresh token endpoint
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        log.info("Token refresh request");
        JwtResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Register endpoint (for intern self-registration)
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register attempt for email: {}", request.getEmail());
        JwtResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    /**
     * Get current logged-in user info
     * GET /api/v1/auth/me
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Change password (for logged-in user)
     * POST /api/v1/auth/change-password
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Change password request");
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    /**
     * Reset password (forgot password flow)
     * POST /api/v1/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request for email: {}", request.getEmail());
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions sent", null));
    }

    /**
     * Logout (client should delete token)
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("Logout request");
        authService.logout();
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}

