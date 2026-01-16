package com.example.backend.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class UserDTOs {

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        private String email;

        @NotBlank(message = "Họ tên không được để trống")
        private String fullName;

        private String phone;

        @Size(min = 1, message = "Phải chọn ít nhất 1 vai trò")
        private List<String> roleCodes;

        // Optional fields for specific roles
        private Long departmentId;
        private String title; // for MENTOR
        private String studentCode; // for INTERN
        private String university; // for INTERN
        private String major; // for INTERN
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private String status;
        private List<String> roles;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class UpdateUserStatusRequest {
        private String status; // ACTIVE, LOCKED
    }

    @Data
    public static class RoleAssignRequest {
        @Size(min = 1, message = "Phải chọn ít nhất 1 vai trò")
        private List<String> roleCodes;
    }
}
