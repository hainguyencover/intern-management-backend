package com.holaho.intern.auth.service;

import com.holaho.intern.shared.dto.request.ChangePasswordRequest;
import com.holaho.intern.shared.dto.request.LoginRequest;
import com.holaho.intern.shared.dto.request.RegisterRequest;
import com.holaho.intern.shared.dto.request.ResetPasswordRequest;
import com.holaho.intern.shared.dto.response.JwtResponse;
import com.holaho.intern.shared.dto.response.UserResponse;
import com.holaho.intern.entity.RefreshToken;
import com.holaho.intern.shared.dto.response.TwoFactorResponse;
import com.holaho.intern.shared.dto.request.TwoFactorVerifyRequest;
import com.holaho.intern.shared.dto.request.ForgotPasswordRequest;

public interface AuthService {
    JwtResponse login(LoginRequest request);

    JwtResponse register(RegisterRequest request);

    UserResponse getCurrentUser();

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout();

    RefreshToken createRefreshToken(Long userId);

    RefreshToken verifyExpiration(RefreshToken token);

    JwtResponse refreshToken(String requestRefreshToken);

    TwoFactorResponse setupTwoFactor(Long userId);

    void verifyAndEnableTwoFactor(Long userId, TwoFactorVerifyRequest request);

    void disableTwoFactor(Long userId);

    void verifyEmail(com.holaho.intern.auth.dto.request.VerifyEmailRequest request);

    void resendVerification(com.holaho.intern.auth.dto.request.ResendVerificationRequest request);
}

