package com.example.backend.dto.request;

import com.example.backend.enums.LeaveType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateLeaveRequest {
    private LocalDate fromDate;
    private LocalDate toDate;
    private LeaveType type;
    private String reason;
}
