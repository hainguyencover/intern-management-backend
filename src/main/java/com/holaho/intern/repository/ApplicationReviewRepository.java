package com.holaho.intern.repository;

import com.holaho.intern.entity.ApplicationReview;
import com.holaho.intern.shared.enums.ReviewDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationReviewRepository extends JpaRepository<ApplicationReview, Long> {
    List<ApplicationReview> findByApplicationId(Long applicationId);

    List<ApplicationReview> findByApplicationIdOrderByDecidedAtDesc(Long applicationId);

    @Query("SELECT ar FROM ApplicationReview ar " +
            "JOIN FETCH ar.application a " +
            "JOIN FETCH ar.reviewer r " +
            "WHERE ar.application.id = :applicationId")
    List<ApplicationReview> findByApplicationIdWithDetails(@Param("applicationId") Long applicationId);

    Optional<ApplicationReview> findFirstByApplicationIdOrderByDecidedAtDesc(Long applicationId);

    List<ApplicationReview> findByReviewerId(Long reviewerId);

    boolean existsByApplicationId(Long applicationId);

    List<ApplicationReview> findByDecision(ReviewDecision decision);

    @Query("SELECT ar FROM ApplicationReview ar " +
            "WHERE ar.decidedAt BETWEEN :startDate AND :endDate")
    List<ApplicationReview> findByDecidedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByReviewerId(Long reviewerId);

    long countByDecision(ReviewDecision decision);
}

