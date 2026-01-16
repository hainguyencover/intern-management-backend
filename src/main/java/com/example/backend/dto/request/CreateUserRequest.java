package com.example.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequest {

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
