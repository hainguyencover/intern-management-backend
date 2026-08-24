package com.holaho.intern.shared.dto.response;

import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.MentorAssignmentType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorAssignmentResponse {

    private Long id;
    private Long enrollmentId;
    private Long internId;
    private String internName;
    private String internEmail;
    private String internCode;
    private Long mentorId;
    private String mentorName;
    private String mentorEmail;
    private String mentorEmployeeCode;
    private Long mentorDepartmentId;
    private String mentorDepartmentName;
    private MentorAssignmentType assignmentType;
    private LocalDate startDate;
    private LocalDate endDate;
    private MentorAssignmentStatus status;
    private String responsibility;
    private LocalDateTime assignedAt;
    private LocalDateTime endedAt;
    private Long assignedById;
    private String assignedByName;
    private LocalDateTime unassignedAt;
    private Long unassignedBy;
    private String unassignedByName;
    private String unassignReason;
    private String reason;
}
