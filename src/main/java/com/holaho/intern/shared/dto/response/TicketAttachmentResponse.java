package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.TicketAttachment;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAttachmentResponse {
    private Long id;
    private Long ticketId;
    private Long commentId;
    private String fileName;
    private String originalFileName;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private String uploadedByName;
    private LocalDateTime createdAt;

    public static TicketAttachmentResponse from(TicketAttachment attachment) {
        if (attachment == null) return null;
        return TicketAttachmentResponse.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket() != null ? attachment.getTicket().getId() : null)
                .commentId(attachment.getComment() != null ? attachment.getComment().getId() : null)
                .fileName(attachment.getFileName())
                .originalFileName(attachment.getOriginalFileName())
                .storageKey(attachment.getStorageKey())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .uploadedByName(attachment.getUploadedBy() != null ? attachment.getUploadedBy().getFullName() : null)
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
