package com.holaho.intern.shared.dto.admin;

import com.holaho.intern.shared.dto.request.CreateUserRequest;
import com.holaho.intern.shared.dto.request.UpdateUserStatusRequest;
import com.holaho.intern.shared.dto.response.UserResponse;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class UserDTOs {

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "Email khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
        @Email(message = "Email khÃƒÂ´ng hÃ¡Â»Â£p lÃ¡Â»â€¡")
        private String email;

        @NotBlank(message = "HÃ¡Â»Â tÃƒÂªn khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
        private String fullName;

        private String phone;

        @Size(min = 1, message = "PhÃ¡ÂºÂ£i chÃ¡Â»Ân ÃƒÂ­t nhÃ¡ÂºÂ¥t 1 vai trÃƒÂ²")
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
        @Size(min = 1, message = "PhÃ¡ÂºÂ£i chÃ¡Â»Ân ÃƒÂ­t nhÃ¡ÂºÂ¥t 1 vai trÃƒÂ²")
        private List<String> roleCodes;
    }
}

