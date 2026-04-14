package com.holaho.intern.entity;

import com.holaho.intern.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "domain")
    private String domain;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

