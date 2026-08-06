package com.holaho.intern.shared.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private PageMeta meta;
    private ApiError error;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiError {
        private String code;
        private String message;
        private Object details;
        private String path;
        private String traceId;
        private LocalDateTime timestamp;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageMeta {
        private int page;
        private int size;
        private int totalPages;
        private long totalElements;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ValidationErrorDetail {
        private String field;
        private String message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<List<T>> successPage(Page<T> page) {
        return ApiResponse.<List<T>>builder()
                .success(true)
                .data(page.getContent())
                .meta(PageMeta.builder()
                        .page(page.getNumber() + 1) // Convert 0-indexed Spring Page to 1-indexed for client
                        .size(page.getSize())
                        .totalPages(page.getTotalPages())
                        .totalElements(page.getTotalElements())
                        .hasNext(page.hasNext())
                        .hasPrevious(page.hasPrevious())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ApiError.builder()
                        .code(mapHttpStatusToCode(status))
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ApiError.builder()
                        .code(mapHttpStatusToCode(status))
                        .message(message)
                        .path(path)
                        .timestamp(LocalDateTime.now())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, Object details, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ApiError.builder()
                        .code(mapHttpStatusToCode(status))
                        .message(message)
                        .details(details)
                        .path(path)
                        .timestamp(LocalDateTime.now())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, Object details, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ApiError.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .path(path)
                        .timestamp(LocalDateTime.now())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, Object details, String path, String traceId) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ApiError.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .path(path)
                        .traceId(traceId)
                        .timestamp(LocalDateTime.now())
                        .build())
                .build();
    }

    private static String mapHttpStatusToCode(int status) {
        switch (status) {
            case 400: return "BAD_REQUEST";
            case 401: return "UNAUTHORIZED";
            case 403: return "FORBIDDEN";
            case 404: return "NOT_FOUND";
            case 409: return "CONFLICT";
            default: return "SYSTEM_ERROR";
        }
    }
}
