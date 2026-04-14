package com.holaho.intern.shared.exception;

import com.holaho.intern.shared.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        public ResponseEntity<ApiResponse<Void>> handleNotFoundException(
                        NotFoundException ex, HttpServletRequest request) {
                log.error("NotFoundException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null, request);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
                        ResourceNotFoundException ex, HttpServletRequest request) {
                log.error("ResourceNotFoundException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null, request);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiResponse<Void>> handleBadRequestException(
                        BadRequestException ex, HttpServletRequest request) {
                log.error("BadRequestException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, request);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
                        IllegalArgumentException ex, HttpServletRequest request) {
                log.error("IllegalArgumentException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, request);
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ApiResponse<Void>> handleConflictException(
                        ConflictException ex, HttpServletRequest request) {
                log.error("ConflictException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), null, request);
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ApiResponse<Void>> handleForbiddenException(
                        ForbiddenException ex, HttpServletRequest request) {
                log.error("ForbiddenException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), null, request);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
                        AccessDeniedException ex, HttpServletRequest request) {
                log.error("AccessDeniedException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này", null,
                                request);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
                        BadCredentialsException ex, HttpServletRequest request) {
                log.error("BadCredentialsException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng", null, request);
        }

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleUsernameNotFoundException(
                        UsernameNotFoundException ex, HttpServletRequest request) {
                log.error("UsernameNotFoundException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng", null, request);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException ex, HttpServletRequest request) {
                log.error("HttpMessageNotReadableException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Yêu cầu gửi lên thiếu dữ liệu payload hoặc sai định dạng", null, request);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });
                log.error("Validation error at {}: {}", request.getRequestURI(), errors);
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", errors, request);
        }

        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ApiResponse<Void>> handleApiException(
                        ApiException ex, HttpServletRequest request) {
                log.error("ApiException: status={}, message={}", ex.status, ex.getMessage());
                return buildErrorResponse(ex.status, ex.getMessage(), null, request);
        }

        @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
                        org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
                log.error("DataIntegrityViolationException: {}", ex.getMessage());
                return buildErrorResponse(HttpStatus.CONFLICT,
                                "Dữ liệu đã tồn tại trong hệ thống. Vui lòng kiểm tra lại.", null,
                                request);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGlobalException(
                        Exception ex, HttpServletRequest request) {
                log.error("Unhandled exception at " + request.getRequestURI(), ex);
                return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau. (" + ex.getClass().getSimpleName() + ")",
                                null, request);
        }

        private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
                        HttpStatus status, String message, Object errors, HttpServletRequest request) {
                ApiResponse<Void> response = ApiResponse.error(status.value(), message, errors,
                                request.getRequestURI());
                return ResponseEntity.status(status).body(response);
        }
}

