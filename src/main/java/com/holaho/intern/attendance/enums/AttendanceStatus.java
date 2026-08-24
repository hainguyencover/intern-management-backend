package com.holaho.intern.attendance.enums;

/**
 * Standardized status for daily attendance records.
 */
public enum AttendanceStatus {
    PRESENT,
    LATE,
    EARLY_LEAVE,
    LATE_AND_EARLY_LEAVE,
    ABSENT,
    ON_LEAVE,
    HOLIDAY,
    INCOMPLETE,
    PENDING_CORRECTION;

    public boolean isPresentOrLate() {
        return this == PRESENT || this == LATE || this == EARLY_LEAVE || this == LATE_AND_EARLY_LEAVE;
    }
}
