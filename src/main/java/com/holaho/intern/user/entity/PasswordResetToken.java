package com.holaho.intern.user.entity;

import com.holaho.intern.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "password_reset_tokens", indexes = {
    @Index(name = "idx_password_reset_token", columnList = "token")
})
public class PasswordResetToken extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "used_at")
    private Instant usedAt;

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
