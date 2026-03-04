package com.example.backend.service;

import com.example.backend.dto.request.ChangePasswordRequest;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.request.ResetPasswordRequest;
import com.example.backend.dto.response.JwtResponse;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.RefreshToken;
import com.example.backend.dto.response.TwoFactorResponse;
import com.example.backend.dto.request.TwoFactorVerifyRequest;

public interface AuthService {
    JwtResponse login(LoginRequest request);

    JwtResponse register(RegisterRequest request);

    UserResponse getCurrentUser();

    void changePassword(ChangePasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout();

    RefreshToken createRefreshToken(Long userId);

    RefreshToken verifyExpiration(RefreshToken token);

    JwtResponse refreshToken(String requestRefreshToken);

    TwoFactorResponse setupTwoFactor(Long userId);

    void verifyAndEnableTwoFactor(Long userId, TwoFactorVerifyRequest request);

    void disableTwoFactor(Long userId);
}
