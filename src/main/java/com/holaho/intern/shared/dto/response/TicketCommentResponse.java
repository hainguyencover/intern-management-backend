package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.TicketComment;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCommentResponse {
    private Long id;
    private Long authorId;
    private String authorName;
    private String content;
    private boolean isInternal;
    private LocalDateTime createdAt;
    private List<TicketAttachmentResponse> attachments;

    public static TicketCommentResponse from(TicketComment comment) {
        return from(comment, null);
    }

    public static TicketCommentResponse from(TicketComment comment, List<TicketAttachmentResponse> attachments) {
        if (comment == null) return null;
        return TicketCommentResponse.builder()
                .id(comment.getId())
                .authorId(comment.getAuthor() != null ? comment.getAuthor().getId() : null)
                .authorName(comment.getAuthor() != null ? comment.getAuthor().getFullName() : null)
                .content(comment.getContent())
                .isInternal(comment.isInternal())
                .createdAt(comment.getCreatedAt())
                .attachments(attachments)
                .build();
    }
}
