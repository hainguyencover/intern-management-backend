package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MentorWorkloadResponse {
    private Long mentorId;

    private String email;
    private String fullName;
    private String phone;

    private String title;
    private String departmentName;

    private Long internCount;
}
