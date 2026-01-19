package com.example.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<ApiError> handleNotFoundException(
                        NotFoundException ex, HttpServletRequest request) {
                log.error("NotFoundException: {}", ex.getMessage());
                ApiError error = ApiError.of(
                                HttpStatus.NOT_FOUND.value(),
                                "NOT_FOUND",
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiError> handleBadRequestException(
                        BadRequestException ex, HttpServletRequest request) {
                log.error("BadRequestException: {}", ex.getMessage());
                ApiError error = ApiError.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "BAD_REQUEST",
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleIllegalArgumentException(
                        IllegalArgumentException ex, HttpServletRequest request) {
                log.error("IllegalArgumentException: {}", ex.getMessage());
                ApiError error = ApiError.of(
                                HttpStatus.BAD_REQUEST.value(),
                                "BAD_REQUEST",
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ApiError> handleConflictException(
                        ConflictException ex, HttpServletRequest request) {
                log.error("ConflictException: {}", ex.getMessage());
                ApiError error = ApiError.of(
                                HttpStatus.CONFLICT.value(),
                                "CONFLICT",
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ApiError> handleForbiddenException(
                        ForbiddenException ex, HttpServletRequest request) {
                log.error("ForbiddenException: {}", ex.getMessage());
                ApiError error = ApiError.of(
                                HttpStatus.FORBIDDEN.value(),
                                "FORBIDDEN",
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDeniedException(
                        AccessDeniedException ex, HttpServletRequest request) {
                log.error("AccessDeniedException: {}", ex.getMessage());
                ApiError error = ApiError.of(
                                HttpStatus.FORBIDDEN.value(),
                                "FORBIDDEN",
                                "Bạn không có quyền truy cập tài nguyên này",
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiError> handleBadCredentialsException(
                        BadCredentialsException ex, HttpServletRequest request) {
                log.error("BadCredentialsException: {}", ex.getMessage());
                ApiError error = ApiError.of(
                                HttpStatus.UNAUTHORIZED.value(),
                                "UNAUTHORIZED",
                                "Email hoặc mật khẩu không đúng",
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationExceptions(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                Map<String, Object> response = new HashMap<>();
                response.put("timestamp", java.time.LocalDateTime.now());
                response.put("status", HttpStatus.BAD_REQUEST.value());
                response.put("error", "VALIDATION_ERROR");
                response.put("message", "Dữ liệu không hợp lệ");
                response.put("errors", errors);
                response.put("path", request.getRequestURI());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ApiError> handleApiException(
                        ApiException ex, HttpServletRequest request) {
                log.error("ApiException: status={}, message={}", ex.status, ex.getMessage());
                ApiError error = ApiError.of(
                                ex.status.value(),
                                ex.status.name(),
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(ex.status).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleGlobalException(
                        Exception ex, HttpServletRequest request) {
                log.error("Unhandled exception", ex);
                ApiError error = ApiError.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "INTERNAL_SERVER_ERROR",
                                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.",
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiError> handleResourceNotFoundException(
                        ResourceNotFoundException ex, HttpServletRequest request) {
                log.error("ResourceNotFoundException: {}", ex.getMessage());
                ApiError error = ApiError.of(
                                HttpStatus.NOT_FOUND.value(),
                                "NOT_FOUND",
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
}
