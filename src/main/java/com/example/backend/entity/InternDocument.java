package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "intern_documents")
public class InternDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;
}
