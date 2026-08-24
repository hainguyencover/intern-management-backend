package com.holaho.intern.mentor.exception;

public class MentorCapacityExceededException extends RuntimeException {
    public MentorCapacityExceededException(String message) {
        super(message);
    }

    public MentorCapacityExceededException(Long mentorId, int current, int capacity) {
        super(String.format("Mentor (ID: %d) capacity exceeded. Current: %d, Capacity: %d", mentorId, current, capacity));
    }
}
