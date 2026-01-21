package com.example.backend.repository;

import com.example.backend.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByIntern_IdAndMentor_IdAndPeriod(Long internId, Long mentorId, String period);

    Page<Evaluation> findByMentor_Id(Long mentorId, Pageable pageable);

    Page<Evaluation> findByIntern_Id(Long internId, Pageable pageable);

    List<Evaluation> findByPeriod(String period);

    List<Evaluation> findByIntern_IdInAndPeriod(List<Long> internIds, String period);
}
