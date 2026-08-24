package com.holaho.intern.evaluation.enums;

/**
 * Lifecycle status of a final evaluation report.
 */
public enum FinalReportStatus {
    DRAFT,
    HR_REVIEWING,
    RETURNED,
    APPROVED,
    PUBLISHED,
    ARCHIVED;

    public boolean isEditable() {
        return this == DRAFT || this == RETURNED;
    }

    public boolean isApprovedOrPublished() {
        return this == APPROVED || this == PUBLISHED;
    }
}
