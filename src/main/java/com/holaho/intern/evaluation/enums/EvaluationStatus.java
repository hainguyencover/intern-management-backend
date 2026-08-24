package com.holaho.intern.evaluation.enums;

/**
 * State machine for evaluation lifecycle.
 *
 * Transitions:
 *   DRAFT → SUBMITTED → HR_REVIEWING → APPROVED → LOCKED
 *                                     → RETURNED → DRAFT (re-edit)
 */
public enum EvaluationStatus {
    DRAFT,
    SUBMITTED,
    HR_REVIEWING,
    RETURNED,
    APPROVED,
    LOCKED;

    public boolean isDraft() {
        return this == DRAFT;
    }

    public boolean isEditable() {
        return this == DRAFT || this == RETURNED;
    }

    public boolean isLocked() {
        return this == SUBMITTED || this == HR_REVIEWING || this == APPROVED || this == LOCKED;
    }

    public boolean isFinal() {
        return this == APPROVED || this == LOCKED;
    }
}
