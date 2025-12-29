package com.example.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.status).body(Map.of(
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

        return ResponseEntity.badRequest().body(Map.of(
                "message", "Validation failed",
                "errors", errors
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegal(IllegalArgumentException ex, HttpServletRequest req) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "path", req.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleAny(Exception ex, HttpServletRequest req) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 500,
                "error", "Internal Server Error",
                "message", ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                "path", req.getRequestURI()
        );
    }
}
