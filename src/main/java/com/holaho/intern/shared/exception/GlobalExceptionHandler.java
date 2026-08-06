package com.holaho.intern.shared.exception;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.ApiResponse.ValidationErrorDetail;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNotFoundException(
                        NotFoundException ex, HttpServletRequest request) {
                log.error("NotFoundException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.NOT_FOUND, ex.getMessage(), null, request);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiResponse<Void>> handleBadRequestException(
                        BadRequestException ex, HttpServletRequest request) {
                log.error("BadRequestException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.INVALID_REQUEST, ex.getMessage(), null, request);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
                        IllegalArgumentException ex, HttpServletRequest request) {
                log.error("IllegalArgumentException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.INVALID_REQUEST, ex.getMessage(), null, request);
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ApiResponse<Void>> handleConflictException(
                        ConflictException ex, HttpServletRequest request) {
                log.error("ConflictException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.CONFLICT, ex.getMessage(), null, request);
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ApiResponse<Void>> handleForbiddenException(
                        ForbiddenException ex, HttpServletRequest request) {
                log.error("ForbiddenException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.FORBIDDEN, ex.getMessage(), null, request);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
                        AccessDeniedException ex, HttpServletRequest request) {
                log.error("AccessDeniedException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này", null,
                                request);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
                        BadCredentialsException ex, HttpServletRequest request) {
                log.error("BadCredentialsException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.BAD_CREDENTIALS, "Email hoặc mật khẩu không đúng", null, request);
        }

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleUsernameNotFoundException(
                        UsernameNotFoundException ex, HttpServletRequest request) {
                log.error("UsernameNotFoundException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.BAD_CREDENTIALS, "Email hoặc mật khẩu không đúng", null, request);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException ex, HttpServletRequest request) {
                log.error("HttpMessageNotReadableException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.INVALID_REQUEST,
                                "Yêu cầu gửi lên thiếu dữ liệu payload hoặc sai định dạng", null, request);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {
                List<ValidationErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> new ValidationErrorDetail(error.getField(), error.getDefaultMessage()))
                                .collect(Collectors.toList());
                log.error("Validation error at {}: {}", request.getRequestURI(), details);
                return buildErrorResponse(ErrorCode.VALIDATION_ERROR, "Dữ liệu xác thực không hợp lệ", details, request);
        }

        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ApiResponse<Void>> handleApiException(
                        ApiException ex, HttpServletRequest request) {
                log.error("ApiException: status={}, message={}", ex.status, ex.getMessage());
                
                // Map HTTP status code back to a generic ErrorCode if possible
                ErrorCode code = mapStatusToErrorCode(ex.status);
                return buildErrorResponse(code, ex.getMessage(), null, request);
        }

        @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
                        org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
                log.error("DataIntegrityViolationException: {}", ex.getMessage());
                return buildErrorResponse(ErrorCode.CONFLICT,
                                "Dữ liệu đã tồn tại trong hệ thống. Vui lòng kiểm tra lại.", null,
                                request);
        }

        @ExceptionHandler(FileStorageException.class)
        public ResponseEntity<ApiResponse<Void>> handleFileStorageException(
                        FileStorageException ex, HttpServletRequest request) {
                log.error("FileStorageException: {}", ex.getMessage(), ex);
                return buildErrorResponse(ErrorCode.SYSTEM_ERROR,
                                "Lỗi xử lý file: " + ex.getMessage(), null, request);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGlobalException(
                        Exception ex, HttpServletRequest request) {
                log.error("Unhandled exception at " + request.getRequestURI(), ex);
                return buildErrorResponse(ErrorCode.SYSTEM_ERROR,
                                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau. (" + ex.getClass().getSimpleName() + ")",
                                null, request);
        }

        private String getTraceId(HttpServletRequest request) {
                String traceId = (String) request.getAttribute("traceId");
                if (traceId == null) {
                        traceId = org.slf4j.MDC.get("traceId");
                }
                if (traceId == null) {
                        traceId = UUID.randomUUID().toString();
                }
                return traceId;
        }

        private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
                        ErrorCode errorCode, String message, Object details, HttpServletRequest request) {
                String traceId = getTraceId(request);
                ApiResponse<Void> response = ApiResponse.error(
                                errorCode.getCode(),
                                message != null ? message : errorCode.getMessage(),
                                details,
                                request.getRequestURI(),
                                traceId
                );
                return ResponseEntity.status(errorCode.getStatus()).body(response);
        }

        private ErrorCode mapStatusToErrorCode(HttpStatus status) {
                if (status == null) return ErrorCode.SYSTEM_ERROR;
                switch (status) {
                        case NOT_FOUND: return ErrorCode.NOT_FOUND;
                        case BAD_REQUEST: return ErrorCode.INVALID_REQUEST;
                        case CONFLICT: return ErrorCode.CONFLICT;
                        case FORBIDDEN: return ErrorCode.FORBIDDEN;
                        case UNAUTHORIZED: return ErrorCode.UNAUTHORIZED;
                        default: return ErrorCode.SYSTEM_ERROR;
                }
        }

        private ErrorCode mapStatusToErrorCode(int statusValue) {
                try {
                        return mapStatusToErrorCode(HttpStatus.valueOf(statusValue));
                } catch (IllegalArgumentException e) {
                        return ErrorCode.SYSTEM_ERROR;
                }
        }
}
