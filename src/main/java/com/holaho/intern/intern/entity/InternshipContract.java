package com.holaho.intern.intern.entity;

import com.holaho.intern.entity.Application;
import com.holaho.intern.shared.entity.BaseEntity;
import com.holaho.intern.shared.enums.ContractStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "internship_contracts",
        uniqueConstraints = @UniqueConstraint(name = "uk_contracts_application_id", columnNames = "application_id")
)
public class InternshipContract extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ContractStatus status = ContractStatus.PENDING_CONFIRMATION;

    @Column(name = "revision_reason", columnDefinition = "TEXT")
    private String revisionReason;

    @Column(name = "revision_requested_at")
    private LocalDateTime revisionRequestedAt;

    @Column(name = "document_hash", length = 64)
    private String documentHash;

    @Column(name = "signed_ip", length = 45)
    private String signedIp;

    @Column(name = "signed_user_agent", length = 500)
    private String signedUserAgent;
}
