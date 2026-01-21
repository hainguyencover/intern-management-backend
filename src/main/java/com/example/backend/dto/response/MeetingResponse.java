package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MeetingResponse {
    private Long id;
    private Long groupId;
    private String groupName;
    private String title;
    private String description;
    private String meetingLink;
    private String location;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
