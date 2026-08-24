package com.holaho.intern.entity;

import com.holaho.intern.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "universities")
public class University extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "email_domain", length = 255)
    private String emailDomain;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";
}
