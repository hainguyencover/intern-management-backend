package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentRequest {
    @NotBlank(message = "Mã phòng ban không được để trống")
    @Size(max = 50, message = "Mã phòng ban không quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên phòng ban không được để trống")
    @Size(max = 255, message = "Tên phòng ban không quá 255 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả không quá 1000 ký tự")
    private String description;
}
