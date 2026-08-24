package com.holaho.intern.mentor.exception;

public class MentorNotFoundException extends RuntimeException {
    public MentorNotFoundException(String message) {
        super(message);
    }

    public MentorNotFoundException(Long id) {
        super("Mentor not found with ID: " + id);
    }
}
