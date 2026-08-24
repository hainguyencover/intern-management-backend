package com.holaho.intern.shared.enums;

public enum TaskStatus {
    OPEN,
    IN_PROGRESS,
    SUBMITTED,
    APPROVED,
    NEEDS_CHANGES,
    DONE,
    CANCELLED;

    public boolean isCompleted() {
        return this == APPROVED || this == DONE;
    }
}

