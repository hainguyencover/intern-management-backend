package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TicketCommentResponse {
    private Long id;
    private Long authorId;
    private String authorEmail;
    private String content;
    private LocalDateTime createdAt;
}
