package com.holaho.intern.mentor.exception;

public class MentorHasActiveAssignmentsException extends RuntimeException {
    private final long activeAssignmentsCount;

    public MentorHasActiveAssignmentsException(Long mentorId, long activeAssignmentsCount) {
        super(String.format("Mentor (ID: %d) currently has %d active intern assignments. Please reassign interns before deactivating.", mentorId, activeAssignmentsCount));
        this.activeAssignmentsCount = activeAssignmentsCount;
    }

    public long getActiveAssignmentsCount() {
        return activeAssignmentsCount;
    }
}
