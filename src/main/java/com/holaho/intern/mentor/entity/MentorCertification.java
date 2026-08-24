package com.holaho.intern.mentor.entity;

import com.holaho.intern.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "mentor_certifications",
        indexes = {
                @Index(name = "idx_mentor_cert_mentor", columnList = "mentor_id")
        }
)
public class MentorCertification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Mentor mentor;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "issuing_organization", length = 200)
    private String issuingOrganization;

    @Column(name = "credential_id", length = 150)
    private String credentialId;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "credential_url", length = 500)
    private String credentialUrl;
}
