package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "intern_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternProfile {
    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String studentCode;
    private String university;
    private String major;
    private Double gpa;
    private String phone;
    private java.sql.Date dob;
    private String address;
    private String cvUrl;
}
