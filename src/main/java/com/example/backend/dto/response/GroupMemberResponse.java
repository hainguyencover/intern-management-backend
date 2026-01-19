package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberResponse {

    private Long id;
    private Long groupId;
    private Long internId;
    private String internName;
    private String studentCode;
    private String internEmail;
    private String university;
    private String major;
    private java.time.LocalDateTime joinedAt;
    private java.time.LocalDateTime leftAt;
}
