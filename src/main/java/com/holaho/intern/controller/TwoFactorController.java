package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.request.TwoFactorVerifyRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.TwoFactorResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/2fa")
@RequiredArgsConstructor
@Tag(name = "Two Factor Authentication", description = "Endpoints for managing 2FA")
public class TwoFactorController {

    private final AuthService authService;

    @PostMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Setup 2FA for the current user")
    public ResponseEntity<ApiResponse<TwoFactorResponse>> setupTwoFactor(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TwoFactorResponse response = authService.setupTwoFactor(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Verify and enable 2FA")
    public ResponseEntity<ApiResponse<Void>> verifyAndEnableTwoFactor(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TwoFactorVerifyRequest request) {
        authService.verifyAndEnableTwoFactor(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Disable 2FA")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.disableTwoFactor(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

