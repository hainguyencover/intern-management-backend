package com.holaho.intern.entity;

import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.user.entity.User;


import com.holaho.intern.shared.entity.BaseEntity;

import com.holaho.intern.shared.enums.AllowanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "allowances",
        indexes = {
                @Index(name = "idx_allowances_intern", columnList = "intern_id"),
                @Index(name = "idx_allowances_month", columnList = "allowance_month"),
                @Index(name = "idx_allowances_status", columnList = "status")
        }
)
public class Allowance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "allowance_month", nullable = false)
    private LocalDate allowanceMonth; // YYYY-MM-01

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllowanceStatus status = AllowanceStatus.PENDING;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private User paidBy;
}

