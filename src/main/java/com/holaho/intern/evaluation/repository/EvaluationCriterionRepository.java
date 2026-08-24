package com.holaho.intern.evaluation.repository;

import com.holaho.intern.evaluation.entity.EvaluationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {

    List<EvaluationCriterion> findByTemplateIdOrderByDisplayOrderAsc(Long templateId);

    long countByTemplateId(Long templateId);

    List<EvaluationCriterion> findByTemplateIdAndRequiredTrue(Long templateId);
}
