package com.holaho.intern.evaluation.repository;

import com.holaho.intern.evaluation.entity.EvaluationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationItemRepository extends JpaRepository<EvaluationItem, Long> {

    List<EvaluationItem> findByEvaluationId(Long evaluationId);

    void deleteByEvaluationId(Long evaluationId);
}
