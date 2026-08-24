package com.holaho.intern.evaluation.enums;

/**
 * Classification of final evaluation score.
 * Used for both individual evaluation and final report grading.
 */
public enum EvaluationClassification {
    EXCELLENT("Xuất sắc"),
    VERY_GOOD("Giỏi"),
    GOOD("Khá"),
    PASS("Đạt"),
    FAIL("Không đạt");

    private final String displayName;

    EvaluationClassification(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determine classification from a score on 0-10 scale.
     */
    public static EvaluationClassification fromScore(double score) {
        if (score >= 9.0) return EXCELLENT;
        if (score >= 8.0) return VERY_GOOD;
        if (score >= 6.5) return GOOD;
        if (score >= 5.0) return PASS;
        return FAIL;
    }
}
