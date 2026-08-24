package com.holaho.intern.user.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.service.AccountActivationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class ActivationController {

    private final AccountActivationService activationService;

    @Data
    public static class ActivateRequest {
        private String token;
        private String newPassword;
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<String>> activateAccount(@RequestBody ActivateRequest request) {
        if (request.getToken() == null || request.getToken().isEmpty()) {
            throw new IllegalArgumentException("Token kích hoạt không được để trống");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có tối thiểu 6 ký tự");
        }

        User user = activationService.activateAccount(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(
                "Kích hoạt tài khoản thành công cho " + user.getEmail(),
                "ACTIVATED"
        ));
    }
}
