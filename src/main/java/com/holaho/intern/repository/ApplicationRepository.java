package com.holaho.intern.repository;

import com.holaho.intern.entity.Application;
import com.holaho.intern.shared.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

        @Query("SELECT a FROM Application a JOIN FETCH a.intern i JOIN FETCH i.user WHERE a.id = :id")
        Optional<Application> findByIdWithIntern(@Param("id") Long id);

        // Ã„â€˜Ã¡Â»Æ’ mapping app.getIntern().getUser() khÃƒÂ´ng bÃ¡Â»â€¹ lazy/N+1 quÃƒÂ¡ nÃ¡ÂºÂ·ng
        @EntityGraph(attributePaths = { "intern", "intern.user" })
        Page<Application> findAll(Pageable pageable);

        @EntityGraph(attributePaths = { "intern", "intern.user" })
        Page<Application> findAllByStatus(ApplicationStatus status, Pageable pageable);

        List<Application> findByIntern_Id(Long internId);

        List<Application> findByStatus(ApplicationStatus status);

        Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

        @Query("SELECT a FROM Application a WHERE a.intern.id = :internId")
        List<Application> findByInternId(@Param("internId") Long internId);

        @Query("SELECT a FROM Application a WHERE a.intern.id = :internId AND a.status = :status")
        List<Application> findByInternIdAndStatus(
                        @Param("internId") Long internId,
                        @Param("status") ApplicationStatus status);

        @Query("SELECT a FROM Application a " +
                        "WHERE a.status = :status " +
                        "AND a.appliedAt BETWEEN :startDate AND :endDate")
        List<Application> findByStatusAndDateRange(
                        @Param("status") ApplicationStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT a FROM Application a JOIN a.intern i JOIN i.user u " +
                        "WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(a.position) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Application> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        @Query("SELECT a FROM Application a " +
                        "LEFT JOIN a.intern ip " +
                        "LEFT JOIN ip.user u " +
                        "WHERE (:status IS NULL OR a.status = :status) " +
                        "AND (:keyword IS NULL OR " +
                        "     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "     LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "     LOWER(a.position) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "ORDER BY a.appliedAt DESC")
        Page<Application> searchApplications(
                        @Param("status") ApplicationStatus status,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        long countByStatus(ApplicationStatus status);

        @Query("SELECT COUNT(a) FROM Application a WHERE a.status = :status " +
                        "AND a.appliedAt >= :fromDate")
        long countByStatusSince(@Param("status") ApplicationStatus status, @Param("fromDate") LocalDateTime fromDate);

        boolean existsByIntern_IdAndStatus(Long internId, ApplicationStatus status);

        boolean existsByIntern_IdAndStatusIn(Long internId, Collection<ApplicationStatus> statuses);

        @Query("SELECT a FROM Application a WHERE a.status = :status ORDER BY a.appliedAt DESC")
        List<Application> findPendingApplications(@Param("status") ApplicationStatus status);
}

