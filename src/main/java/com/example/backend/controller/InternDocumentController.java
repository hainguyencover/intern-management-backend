package com.example.backend.controller;

import com.example.backend.dto.response.InternDocumentResponse;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.InternDocumentRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.InternDocumentService;
import com.example.backend.service.StorageService;
import com.example.backend.enums.DocumentType;
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

@RestController
@RequestMapping("/api")
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
        if (fileUrl == null || fileUrl.isBlank()) return "document.pdf";
        int idx = fileUrl.lastIndexOf('/');
        String name = (idx >= 0) ? fileUrl.substring(idx + 1) : fileUrl;
        if (name.isBlank()) return "document.pdf";
        return name;
    }

    private boolean isPdf(MultipartFile file) {
        if (file == null) return false;
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);

        // chấp nhận cả theo filename và content-type
        return name.endsWith(".pdf") || ct.contains("pdf");
    }

    private boolean isPdfFilename(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    /**
     * Check quyền download document:
     * - HR/ADMIN: được download mọi doc
     * - INTERN: chỉ download doc thuộc intern profile của chính mình
     */
    private void authorizeDownload(Long documentId, User user) {
        Long userId = user.getId();
        boolean isHrOrAdmin = hasRole(userId, "HR") || hasRole(userId, "ADMIN");
        if (isHrOrAdmin) return;

        boolean isIntern = hasRole(userId, "INTERN");
        if (!isIntern) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to download documents");
        }

        // Intern chỉ được download doc của chính mình
        var ip = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern profile not found for userId=" + userId));

        boolean owns = internDocumentRepository.existsByIdAndIntern_Id(documentId, ip.getId());
        if (!owns) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can download only your own documents");
        }
    }

    // ----------------------------
    // INTERN: list + upload
    // ----------------------------
    @GetMapping("/intern/documents")
    public ResponseEntity<?> getMyDocuments() {
        User user = currentUserOrThrow();
        Long userId = user.getId();

        boolean isIntern = hasRole(userId, "INTERN");
        if (!isIntern) {
            boolean isHrOrAdmin = hasRole(userId, "HR") || hasRole(userId, "ADMIN");
            if (isHrOrAdmin) {
                return ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .header("Location", "/api/hr/interns")
                        .body(java.util.Map.of(
                                "message", "Redirecting HR/Admin to /api/hr/interns to select an intern.",
                                "redirect", "/api/hr/interns"));
            }
            throw new ApiException(HttpStatus.FORBIDDEN, "Only INTERN can access /api/intern/documents");
        }

        var ip = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern profile not found for userId=" + userId));

        List<InternDocumentResponse> docs = documentService.getMyDocuments(ip.getId());
        return ResponseEntity.ok(docs);
    }

    @PostMapping(path = {"/intern/documents", "/intern/documents/upload", "/documents/upload"})
    public ResponseEntity<InternDocumentResponse> uploadMyDocument(
            @RequestParam("type") DocumentType type,
            @RequestParam("file") MultipartFile file
    ) {
        User user = currentUserOrThrow();
        Long userId = user.getId();

        if (!hasRole(userId, "INTERN")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only INTERN can upload documents");
        }

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is required");
        }

        // ✅ Chặn chỉ cho upload PDF
        if (!isPdf(file)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF files are allowed");
        }

        var ip = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern profile not found for userId=" + userId));

        DocumentType docType;
        try {
            docType = DocumentType.valueOf(type.strip());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid document type: '" + type + "'");
        }

        InternDocumentResponse resp = documentService.uploadForIntern(ip.getId(), docType, file);
        return ResponseEntity.ok(resp);
    }

    // ----------------------------
    // HR: view intern documents
    // ----------------------------
    @GetMapping("/hr/interns/{id}/documents")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<List<InternDocumentResponse>> getInternDocumentsForHr(@PathVariable("id") Long internId) {
        List<InternDocumentResponse> docs = documentService.getDocumentsOfIntern(internId);
        return ResponseEntity.ok(docs);
    }

    // ----------------------------
    // Download: shared logic
    // ----------------------------
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("id") Long documentId) {
        User user = currentUserOrThrow();

        // ✅ check quyền
        authorizeDownload(documentId, user);

        var doc = internDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        String fileUrl = doc.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Document has no fileUrl");
        }

        String filename = filenameFromFileUrl(fileUrl);
        boolean pdf = isPdfFilename(filename);

        // (Tuỳ chọn) HR chỉ cho tải PDF — nếu bạn muốn strict
        // boolean isHr = hasRole(user.getId(), "HR");
        // if (isHr && !pdf) throw new ApiException(HttpStatus.NOT_ACCEPTABLE, "HR can download only PDF files");

        Resource resource = storage.loadFileAsResource(fileUrl);

        MediaType contentType = pdf ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(contentType)
                .body(resource);
    }

    // ✅ alias để FE gọi /api/documents/download/{id}
    @GetMapping("/documents/download/{id}")
    public ResponseEntity<Resource> downloadDocumentAlias(@PathVariable("id") Long documentId) {
        return downloadDocument(documentId);
    }

    // ✅ HR alias đúng URL FE đang gọi: /api/hr/documents/download/{id}
    @GetMapping({"/hr/documents/download/{id}", "/hr/documents/{id}/download"})
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<Resource> hrDownload(@PathVariable("id") Long documentId) {
        return downloadDocument(documentId);
    }

    // ----------------------------
    // Status + approve/reject
    // ----------------------------
    @GetMapping("/documents/{id}/status")
    public ResponseEntity<?> getDocumentStatus(@PathVariable("id") Long documentId) {
        var doc = internDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        return ResponseEntity.ok(java.util.Map.of(
                "id", doc.getId(),
                "status", doc.getStatus(),
                "reviewedById", doc.getReviewedBy() != null ? doc.getReviewedBy().getId() : null,
                "reviewedAt", doc.getReviewedAt(),
                "reviewNote", doc.getReviewNote()
        ));
    }

    @PostMapping("/hr/documents/{id}/approve")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternDocumentResponse> approveDocument(@PathVariable("id") Long documentId) {
        User user = currentUserOrThrow();
        InternDocumentResponse resp = documentService.approve(documentId, user.getId());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/hr/documents/{id}/reject")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternDocumentResponse> rejectDocument(
            @PathVariable("id") Long documentId,
            @RequestParam(value = "note", required = false) String note
    ) {
        User user = currentUserOrThrow();
        InternDocumentResponse resp = documentService.reject(documentId, user.getId(), note);
        return ResponseEntity.ok(resp);
    }

    // legacy PUT endpoints
    @PutMapping("/documents/{id}/approve")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternDocumentResponse> approveDocumentPut(
            @PathVariable("id") Long documentId,
            @RequestParam(value = "hrUserId", required = false) Long hrUserId
    ) {
        Long actingHrId = hrUserId;
        if (actingHrId == null) {
            User user = currentUserOrThrow();
            actingHrId = user.getId();
        }
        InternDocumentResponse resp = documentService.approve(documentId, actingHrId);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/documents/{id}/reject")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternDocumentResponse> rejectDocumentPut(
            @PathVariable("id") Long documentId,
            @RequestParam(value = "hrUserId", required = false) Long hrUserId,
            @RequestParam(value = "note", required = false) String note
    ) {
        Long actingHrId = hrUserId;
        if (actingHrId == null) {
            User user = currentUserOrThrow();
            actingHrId = user.getId();
        }
        InternDocumentResponse resp = documentService.reject(documentId, actingHrId, note);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/hr/interns/{internId}/documents/contracts")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<InternDocumentResponse> uploadInternshipContract(@PathVariable("internId") Long internId,
                                                                           @RequestParam("file") MultipartFile file) {
        internProfileRepository.findById(internId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern profile not found: " + internId));
        InternDocumentResponse resp = documentService.uploadForIntern(
                internId,
                DocumentType.INTERNSHIP_CONTRACT,
                file
        );

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/intern/documents/{id}/confirm")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<InternDocumentResponse> confirmMyContract(@PathVariable("id") Long documentId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + email));

        InternProfile ip = internProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern profile not found"));

        InternDocumentResponse resp = documentService.confirmContract(ip.getId(), documentId);
        return ResponseEntity.ok(resp);
    }
}
