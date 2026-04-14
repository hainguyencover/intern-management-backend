package com.holaho.intern.shared.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorResponse {
    private Long id; // mentorId
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String title;
    private Long departmentId;
    private String departmentName;
    private LocalDateTime createdAt;
    private Long internCount;
}

