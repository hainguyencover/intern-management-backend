package com.example.backend.repository;

import com.example.backend.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

        List<Evaluation> findByIntern_IdOrderByCreatedAtDesc(Long internId);

        List<Evaluation> findByMentorIdOrderByCreatedAtDesc(Long mentorId);

        List<Evaluation> findByInternIdAndPeriod(Long internId, String period);

        boolean existsByIntern_IdAndMentor_IdAndPeriod(Long internId, Long mentorId, String period);

        List<Evaluation> findByInternId(Long internId);

        List<Evaluation> findByInternIdOrderByCreatedAtDesc(Long internId);

        @Query("SELECT e FROM Evaluation e " +
                        "JOIN FETCH e.intern i " +
                        "JOIN FETCH i.user " +
                        "JOIN FETCH e.mentor m " +
                        "JOIN FETCH m.user " +
                        "WHERE e.intern.id = :internId")
        List<Evaluation> findByInternIdWithDetails(@Param("internId") Long internId);

        List<Evaluation> findByMentorId(Long mentorId);

        Page<Evaluation> findByMentor_Id(Long mentorId, Pageable pageable);

        Optional<Evaluation> findFirstByInternIdAndPeriodOrderByCreatedAtDesc(
                        Long internId, String period);

        @Query("SELECT AVG(e.score) FROM Evaluation e WHERE e.intern.id = :internId")
        Double findAverageScoreByInternId(@Param("internId") Long internId);

        @Query("SELECT AVG(e.score) FROM Evaluation e WHERE e.mentor.id = :mentorId")
        Double findAverageScoreByMentorId(@Param("mentorId") Long mentorId);

        long countByInternId(Long internId);

        long countByMentorId(Long mentorId);

        boolean existsByInternIdAndPeriod(Long internId, String period);

        @Query("select avg(e.score) from Evaluation e where e.intern.id = :internId")
        Double avgScoreByIntern(@Param("internId") Long internId);

        @Query("SELECT e FROM Evaluation e WHERE e.mentor.id = :mentorId " +
                        "AND (:period IS NULL OR e.period = :period) " +
                        "AND (:keyword IS NULL OR LOWER(e.intern.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.intern.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
                        +
                        "ORDER BY e.createdAt DESC")
        Page<Evaluation> findByMentorIdAndFilters(
                        @Param("mentorId") Long mentorId,
                        @Param("period") String period,
                        @Param("keyword") String keyword,
                        Pageable pageable);
}
