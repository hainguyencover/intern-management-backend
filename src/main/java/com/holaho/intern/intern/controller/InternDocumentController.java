package com.holaho.intern.intern.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.InternDocumentResponse;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.exception.ApiException;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.intern.service.InternDocumentService;
import com.holaho.intern.service.StorageService;
import com.holaho.intern.shared.enums.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InternDocumentController {

    private final InternDocumentService documentService;
    private final UserRepository userRepository;
    private final InternDocumentRepository internDocumentRepository;
    private final InternProfileRepository internProfileRepository;
    private final StorageService storage;

    // ----------------------------
    // Helpers
    // ----------------------------
    private User currentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + email));
    }

    private boolean hasRole(Long userId, String roleCode) {
        return userRepository.existsByIdAndRoleCode(userId, roleCode);
    }

    private String filenameFromFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank())
            return "document.pdf";
        int idx = fileUrl.lastIndexOf('/');
        String name = (idx >= 0) ? fileUrl.substring(idx + 1) : fileUrl;
        if (name.isBlank())
            return "document.pdf";
        return name;
    }

    private boolean isPdf(MultipartFile file) {
        if (file == null)
            return false;
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        return name.endsWith(".pdf") || ct.contains("pdf");
    }

    private boolean isPdfFilename(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private void authorizeDownload(Long documentId, User user) {
        Long userId = user.getId();
        boolean isHrOrAdmin = hasRole(userId, "HR") || hasRole(userId, "ADMIN");
        if (isHrOrAdmin)
            return;

        boolean isIntern = hasRole(userId, "INTERN");
        if (!isIntern) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền tải tài liệu này");
        }

        var ip = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin thực tập sinh"));

        boolean owns = internDocumentRepository.existsByIdAndIntern_Id(documentId, ip.getId());
        if (!owns) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn chỉ có thể tải tài liệu của chính mình");
        }
    }

    // ----------------------------
    // INTERN: list + upload
    // ----------------------------
    @GetMapping("/intern/documents")
    public ResponseEntity<ApiResponse<List<InternDocumentResponse>>> getMyDocuments() {
        User user = currentUserOrThrow();
        Long userId = user.getId();

        if (!hasRole(userId, "INTERN")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ thực tập sinh mới có quyền truy cập");
        }

        var ip = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin thực tập sinh"));

        List<InternDocumentResponse> docs = documentService.getMyDocuments(ip.getId());
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @PostMapping(path = { "/intern/documents", "/intern/documents/upload", "/documents/upload" })
    public ResponseEntity<ApiResponse<InternDocumentResponse>> uploadMyDocument(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "internId", required = false) Long internIdParam) {
        User user = currentUserOrThrow();
        Long userId = user.getId();
        Long targetInternId;

        if (hasRole(userId, "INTERN")) {
            var ip = internProfileRepository.findByUser_Id(userId)
                    .orElseThrow(
                            () -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin thực tập sinh"));
            targetInternId = ip.getId();
        } else if (hasRole(userId, "HR") || hasRole(userId, "ADMIN")) {
            if (internIdParam == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "HR/Admin phải cung cấp internId để tải lên tài liệu");
            }
            targetInternId = internIdParam;
            if (!internProfileRepository.existsById(targetInternId)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ thực tập sinh: " + targetInternId);
            }
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "Quyền truy cập bị từ chối");
        }

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng chọn file");
        }

        if (!isPdf(file)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Chỉ hỗ trợ file định dạng PDF");
        }

        if (type == null || type.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Loại tài liệu là bắt buộc");
        }

        DocumentType docType;
        try {
            docType = DocumentType.valueOf(type.strip());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Loại tài liệu không hợp lệ: '" + type + "'");
        }

        InternDocumentResponse resp = documentService.uploadForIntern(targetInternId, docType, file, userId);
        return ResponseEntity.ok(ApiResponse.success("Tải lên tài liệu thành công", resp));
    }

    // ----------------------------
    // HR: view intern documents
    // ----------------------------
    @GetMapping("/hr/interns/{id}/documents")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<List<InternDocumentResponse>>> getInternDocumentsForHr(
            @PathVariable("id") Long internId) {
        List<InternDocumentResponse> docs = documentService.getDocumentsOfIntern(internId);
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    // ----------------------------
    // Download: shared logic
    // ----------------------------
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("id") Long documentId,
            @RequestParam(required = false, defaultValue = "false") boolean inline) {
        User user = currentUserOrThrow();
        authorizeDownload(documentId, user);

        var doc = internDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài liệu không tồn tại: " + documentId));

        String fileUrl = doc.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Tài liệu không có đường dẫn file");
        }

        String filename = filenameFromFileUrl(fileUrl);
        boolean pdf = isPdfFilename(filename);
        Resource resource = storage.loadFileAsResource(fileUrl);
        MediaType contentType = pdf ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
        String disposition = inline ? "inline" : "attachment";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(resource);
    }

    @GetMapping("/documents/download/{id}")
    public ResponseEntity<Resource> downloadDocumentAlias(@PathVariable("id") Long documentId,
            @RequestParam(required = false, defaultValue = "false") boolean inline) {
        return downloadDocument(documentId, inline);
    }

    @GetMapping({ "/hr/documents/download/{id}", "/hr/documents/{id}/download" })
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<Resource> hrDownload(@PathVariable("id") Long documentId,
            @RequestParam(required = false, defaultValue = "false") boolean inline) {
        return downloadDocument(documentId, inline);
    }

    // ----------------------------
    // Status + approve/reject
    // ----------------------------
    @GetMapping("/documents/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDocumentStatus(@PathVariable("id") Long documentId) {
        var doc = internDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài liệu không tồn tại: " + documentId));

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", doc.getId(),
                "status", doc.getStatus(),
                "reviewedById", doc.getReviewedBy() != null ? doc.getReviewedBy().getId() : null,
                "reviewedAt", doc.getReviewedAt(),
                "reviewNote", doc.getReviewNote() != null ? doc.getReviewNote() : "")));
    }

    @PostMapping("/hr/documents/{id}/approve")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<InternDocumentResponse>> approveDocument(@PathVariable("id") Long documentId) {
        User user = currentUserOrThrow();
        InternDocumentResponse resp = documentService.approve(documentId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt tài liệu thành công", resp));
    }

    @PostMapping("/hr/documents/{id}/reject")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<InternDocumentResponse>> rejectDocument(
            @PathVariable("id") Long documentId,
            @RequestParam(value = "note", required = false) String note) {
        User user = currentUserOrThrow();
        InternDocumentResponse resp = documentService.reject(documentId, user.getId(), note);
        return ResponseEntity.ok(ApiResponse.success("Từ chối tài liệu thành công", resp));
    }

    @PostMapping(value = "/hr/interns/{internId}/documents/contracts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InternDocumentResponse>> uploadInternshipContract(
            @PathVariable("internId") Long internId,
            @RequestParam("file") MultipartFile file) {
        internProfileRepository.findById(internId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy hồ sơ thực tập sinh: " + internId));
        User user = currentUserOrThrow();
        InternDocumentResponse resp = documentService.uploadForIntern(
                internId,
                DocumentType.INTERNSHIP_CONTRACT,
                file,
                user.getId());

        return ResponseEntity.ok(ApiResponse.success("Tải lên hợp đồng thực tập thành công", resp));
    }

    @PostMapping("/intern/documents/{id}/confirm")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<InternDocumentResponse>> confirmMyContract(@PathVariable("id") Long documentId) {
        User user = currentUserOrThrow();
        InternProfile ip = internProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ thực tập sinh"));

        InternDocumentResponse resp = documentService.confirmContract(ip.getId(), documentId);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận hợp đồng thành công", resp));
    }
}
