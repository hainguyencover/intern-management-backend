package com.holaho.intern.entity;

import com.holaho.intern.shared.entity.BaseEntity;
import com.holaho.intern.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "university_users", uniqueConstraints = @UniqueConstraint(name = "uk_univ_user_pair", columnNames = {"university_id", "user_id"}))
public class UniversityUser extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
