package com.holaho.intern.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // System Errors
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYSTEM_ERROR", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Yêu cầu gửi lên không hợp lệ."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu xác thực không hợp lệ."),
    
    // Auth Errors
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Phiên đăng nhập hết hạn hoặc bạn chưa đăng nhập."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền truy cập tài nguyên này."),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Email hoặc mật khẩu không chính xác."),
    
    // Resource Errors
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy tài nguyên yêu cầu."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "Dữ liệu đã tồn tại trong hệ thống.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
