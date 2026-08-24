package com.holaho.intern.service;

import com.holaho.intern.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

@Service
public class DocumentValidationService {

    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx");

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/x-pdf",
            "application/octet-stream"
    );

    /**
     * Validates file size, extension, MIME type, and magic bytes.
     */
    public void validateDocumentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File upload không được để rỗng.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Dung lượng file vượt quá giới hạn tối đa 10MB (Kích thước hiện tại: " +
                            String.format("%.2f", file.getSize() / (1024.0 * 1024.0)) + "MB).");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tên file không hợp lệ hoặc thiếu định dạng mở rộng.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Định dạng file ." + extension + " không được hỗ trợ. Chỉ chấp nhận PDF hoặc DOCX.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String cleanContentType = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (!ALLOWED_CONTENT_TYPES.contains(cleanContentType)) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Content-Type '" + contentType + "' không hợp lệ. Chỉ chấp nhận PDF hoặc DOCX.");
            }
        }

        validateMagicBytes(file, extension);
    }

    private void validateMagicBytes(MultipartFile file, String extension) {
        byte[] header = new byte[8];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header, 0, header.length);
            if (read < 4) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Nội dung file không hợp lệ hoặc bị lỗi.");
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc nội dung file: " + e.getMessage());
        }

        if ("pdf".equals(extension)) {
            // PDF Magic bytes: %PDF- (0x25 0x50 0x44 0x46)
            if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Cấu trúc file không phải là PDF hợp lệ (Magic Bytes mismatch).");
            }
        } else if ("docx".equals(extension)) {
            // DOCX Zip format Magic bytes: PK\x03\x04 (0x50 0x4B 0x03 0x04)
            if (header[0] != 0x50 || header[1] != 0x4B || header[2] != 0x03 || header[3] != 0x04) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Cấu trúc file không phải là DOCX hợp lệ (Magic Bytes mismatch).");
            }
        }
    }
}
