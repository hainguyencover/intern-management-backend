package com.holaho.intern.evaluation.repository;

import com.holaho.intern.evaluation.entity.EvaluationTemplate;
import com.holaho.intern.evaluation.enums.EvaluationPeriod;
import com.holaho.intern.evaluation.enums.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationTemplateRepository extends JpaRepository<EvaluationTemplate, Long> {

    List<EvaluationTemplate> findByTenantIdAndStatus(Long tenantId, TemplateStatus status);

    @Query("SELECT t FROM EvaluationTemplate t LEFT JOIN FETCH t.criteria " +
           "WHERE t.id = :id")
    Optional<EvaluationTemplate> findByIdWithCriteria(@Param("id") Long id);

    /**
     * Find active template for a specific program.
     */
    @Query("SELECT t FROM EvaluationTemplate t LEFT JOIN FETCH t.criteria " +
           "WHERE t.tenantId = :tenantId " +
           "AND t.program.id = :programId " +
           "AND t.status = 'ACTIVE' " +
           "ORDER BY t.version DESC")
    List<EvaluationTemplate> findActiveByProgramId(
            @Param("tenantId") Long tenantId,
            @Param("programId") Long programId);

    /**
     * Find default active template (no program association).
     */
    @Query("SELECT t FROM EvaluationTemplate t LEFT JOIN FETCH t.criteria " +
           "WHERE t.tenantId = :tenantId " +
           "AND t.program IS NULL " +
           "AND t.status = 'ACTIVE' " +
           "ORDER BY t.version DESC")
    List<EvaluationTemplate> findDefaultActive(@Param("tenantId") Long tenantId);
}
