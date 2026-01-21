package com.example.backend.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateMeetingRequest {
    private Long groupId;
    private String title;
    private String description;
    private String meetingLink;
    private String location;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
