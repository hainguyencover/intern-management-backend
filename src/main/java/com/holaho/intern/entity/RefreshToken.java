package com.holaho.intern.entity;

import com.holaho.intern.user.entity.User;


import com.holaho.intern.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "replaced_by_token")
    private String replacedByToken;

}

