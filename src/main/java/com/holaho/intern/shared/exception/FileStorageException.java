package com.holaho.intern.shared.exception;

/**
 * Exception thrown when file storage or export operations fail.
 * Maps to HTTP 500 (Internal Server Error) in GlobalExceptionHandler.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
