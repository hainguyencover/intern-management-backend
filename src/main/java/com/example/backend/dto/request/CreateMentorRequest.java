package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMentorRequest {

    @NotNull(message = "User ID không được để trống")
    private Long userId;

    private Long departmentId;

    @Size(max = 255, message = "Chức danh không quá 255 ký tự")
    private String title;
}







