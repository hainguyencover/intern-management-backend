package com.example.backend.dto.request;

import lombok.Data;

@Data
public class MentorCreateRequest {
    private String email;
    private String password;
    private String fullName;
    private String phone;

    private Long departmentId;   // optional
    private String title;        // optional
}
