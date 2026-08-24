package com.holaho.intern.shared.dto.response;

import com.holaho.intern.shared.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private Long id;
    private Long programId;
    private String programName;
    private String programCode;
    private Long internId;
    private String internName;
    private String internEmail;
    private String studentCode;
    private String university;
    private String major;
    private Long groupId;
    private String groupName;
    private Long mentorId;
    private String mentorName;
    private EnrollmentStatus status;
    private LocalDate joinedAt;
    private LocalDate endedAt;
}
