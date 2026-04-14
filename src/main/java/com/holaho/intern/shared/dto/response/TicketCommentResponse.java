package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.TicketComment;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketCommentResponse {
    private Long id;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;

    public static TicketCommentResponse from(TicketComment comment) {
        return TicketCommentResponse.builder()
                .id(comment.getId())
                .authorName(comment.getAuthor().getFullName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}

