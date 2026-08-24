package com.holaho.intern.user.entity;

import com.holaho.intern.shared.entity.BaseEntity;

import com.holaho.intern.shared.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User extends BaseEntity {

        @Column(nullable = false, length = 255)
        private String email;

        @Column(name = "password_hash", nullable = false, length = 255)
        private String passwordHash;

        @Column(name = "full_name", nullable = false, length = 255)
        private String fullName;

        @Column(length = 50)
        private String phone;

        @Column(length = 100)
        private String address;

        @Column(name = "email_verified", nullable = false)
        private Boolean emailVerified = false;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private UserStatus status = UserStatus.ACTIVE;

        @org.hibernate.annotations.BatchSize(size = 30)
        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
        private Set<Role> roles = new HashSet<>();

        @Column(name = "two_factor_secret")
        private String twoFactorSecret;

        @Column(name = "is_two_factor_enabled")
        private Boolean isTwoFactorEnabled = false;

        @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
        private Set<UserPermission> userPermissions = new HashSet<>();

        @Column(name = "failed_attempts", nullable = false)
        private Integer failedAttempts = 0;

        @Column(name = "lock_time")
        private java.time.Instant lockTime;

        @Column(name = "security_version", nullable = false)
        private Integer securityVersion = 1;
}

