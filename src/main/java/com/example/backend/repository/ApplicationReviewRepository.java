package com.example.backend.repository;

import com.example.backend.entity.ApplicationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationReviewRepository extends JpaRepository<ApplicationReview, Long> {
    List<ApplicationReview> findByApplicationIdOrderByDecidedAtDesc(Long applicationId);
}
