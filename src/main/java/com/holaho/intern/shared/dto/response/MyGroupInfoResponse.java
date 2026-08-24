package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyGroupInfoResponse {

    private Long programId;
    private String programName;
    private String programCode;
    private LocalDate programStartDate;
    private LocalDate programEndDate;

    private Long groupId;
    private String groupName;
    private String groupDescription;

    private Long mentorId;
    private String mentorName;
    private String mentorEmail;
    private String mentorPhone;
    private String mentorDepartment;

    private List<CoInternDto> coInterns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoInternDto {
        private Long internId;
        private String fullName;
        private String studentCode;
        private String email;
        private String university;
        private String major;
        private LocalDateTime joinedAt;
    }
}
