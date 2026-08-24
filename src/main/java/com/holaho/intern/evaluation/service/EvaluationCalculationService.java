package com.holaho.intern.evaluation.service;

import com.holaho.intern.evaluation.entity.EvaluationItem;
import com.holaho.intern.evaluation.enums.EvaluationClassification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculates evaluation scores from individual criterion items.
 * All calculation logic is centralized here — frontend must NOT calculate scores.
 */
@Service
@Slf4j
public class EvaluationCalculationService {

    /**
     * Calculate the overall weighted score from evaluation items.
     *
     * Formula: Σ(score × weight / 100)
     * Each item's weight is stored as a percentage (e.g., 15.00 = 15%).
     * Score is on a 0-10 scale.
     *
     * Example:
     *   Problem Solving: score=8.5, weight=10% → contribution = 8.5 × 10/100 = 0.85
     *   Sum of all contributions = overall score (0-10 scale)
     */
    public BigDecimal calculateOverallScore(List<EvaluationItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = items.stream()
                .filter(item -> item.getScore() != null && item.getWeightSnapshot() != null)
                .map(EvaluationItem::getWeightedContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Determine the classification from a score on 0-10 scale.
     */
    public EvaluationClassification determineClassification(BigDecimal score) {
        if (score == null) return EvaluationClassification.FAIL;
        return EvaluationClassification.fromScore(score.doubleValue());
    }

    /**
     * Validate that all weights in the items sum to approximately 100%.
     */
    public boolean validateWeights(List<EvaluationItem> items) {
        if (items == null || items.isEmpty()) return false;
        double totalWeight = items.stream()
                .mapToDouble(i -> i.getWeightSnapshot().doubleValue())
                .sum();
        return Math.abs(totalWeight - 100.0) < 0.01;
    }

    /**
     * Check if all required criteria have been scored.
     */
    public boolean allRequiredCriteriaScored(List<EvaluationItem> items) {
        if (items == null || items.isEmpty()) return false;
        return items.stream()
                .filter(item -> item.getCriterion() != null && Boolean.TRUE.equals(item.getCriterion().getRequired()))
                .allMatch(item -> item.getScore() != null);
    }
}
