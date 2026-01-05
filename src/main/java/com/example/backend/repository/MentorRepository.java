package com.example.backend.repository;

import com.example.backend.entity.Mentor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MentorRepository extends JpaRepository<Mentor, Long> {
    Optional<Mentor> findByUser_Id(Long userId);
    Optional<Mentor> findByUser_Email(String email);
    boolean existsByUser_Id(Long userId);
}
