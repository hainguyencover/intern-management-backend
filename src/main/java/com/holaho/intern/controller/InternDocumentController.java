package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.InternDocumentResponse;
import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.entity.User;
import com.holaho.intern.shared.exception.ApiException;
import com.holaho.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.InternDocumentRepository;
import com.holaho.intern.repository.UserRepository;
import com.holaho.intern.service.InternDocumentService;
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
            throw new ApiException(HttpStatus.FORBIDDEN, "BÃƒÂ¡Ã‚ÂºÃ‚Â¡n khÃƒÆ’Ã‚Â´ng cÃƒÆ’Ã‚Â³ quyÃƒÂ¡Ã‚Â»Ã‚Ân tÃƒÂ¡Ã‚ÂºÃ‚Â£i tÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u nÃƒÆ’Ã‚Â y");
        }

        var ip = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KhÃƒÆ’Ã‚Â´ng tÃƒÆ’Ã‚Â¬m thÃƒÂ¡Ã‚ÂºÃ‚Â¥y thÃƒÆ’Ã‚Â´ng tin thÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â­p sinh"));

        boolean owns = internDocumentRepository.existsByIdAndIntern_Id(documentId, ip.getId());
        if (!owns) {
            throw new ApiException(HttpStatus.FORBIDDEN, "BÃƒÂ¡Ã‚ÂºÃ‚Â¡n chÃƒÂ¡Ã‚Â»Ã¢â‚¬Â° cÃƒÆ’Ã‚Â³ thÃƒÂ¡Ã‚Â»Ã†â€™ tÃƒÂ¡Ã‚ÂºÃ‚Â£i tÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u cÃƒÂ¡Ã‚Â»Ã‚Â§a chÃƒÆ’Ã‚Â­nh mÃƒÆ’Ã‚Â¬nh");
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
            throw new ApiException(HttpStatus.FORBIDDEN, "ChÃƒÂ¡Ã‚Â»Ã¢â‚¬Â° thÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â­p sinh mÃƒÂ¡Ã‚Â»Ã¢â‚¬Âºi cÃƒÆ’Ã‚Â³ quyÃƒÂ¡Ã‚Â»Ã‚Ân truy cÃƒÂ¡Ã‚ÂºÃ‚Â­p");
        }

        var ip = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KhÃƒÆ’Ã‚Â´ng tÃƒÆ’Ã‚Â¬m thÃƒÂ¡Ã‚ÂºÃ‚Â¥y thÃƒÆ’Ã‚Â´ng tin thÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â­p sinh"));

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
                            () -> new ApiException(HttpStatus.NOT_FOUND, "KhÃƒÆ’Ã‚Â´ng tÃƒÆ’Ã‚Â¬m thÃƒÂ¡Ã‚ÂºÃ‚Â¥y thÃƒÆ’Ã‚Â´ng tin thÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â­p sinh"));
            targetInternId = ip.getId();
        } else if (hasRole(userId, "HR") || hasRole(userId, "ADMIN")) {
            if (internIdParam == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "HR/Admin phÃƒÂ¡Ã‚ÂºÃ‚Â£i cung cÃƒÂ¡Ã‚ÂºÃ‚Â¥p internId Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã†â€™ tÃƒÂ¡Ã‚ÂºÃ‚Â£i lÃƒÆ’Ã‚Âªn tÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u");
            }
            targetInternId = internIdParam;
            if (!internProfileRepository.existsById(targetInternId)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "KhÃƒÆ’Ã‚Â´ng tÃƒÆ’Ã‚Â¬m thÃƒÂ¡Ã‚ÂºÃ‚Â¥y hÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ sÃƒâ€ Ã‚Â¡ thÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â­p sinh: " + targetInternId);
            }
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "QuyÃƒÂ¡Ã‚Â»Ã‚Ân truy cÃƒÂ¡Ã‚ÂºÃ‚Â­p bÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¹ tÃƒÂ¡Ã‚Â»Ã‚Â« chÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœi");
        }

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lÃƒÆ’Ã‚Â²ng chÃƒÂ¡Ã‚Â»Ã‚Ân file");
        }

        if (!isPdf(file)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "ChÃƒÂ¡Ã‚Â»Ã¢â‚¬Â° hÃƒÂ¡Ã‚Â»Ã¢â‚¬â€ trÃƒÂ¡Ã‚Â»Ã‚Â£ file Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¹nh dÃƒÂ¡Ã‚ÂºÃ‚Â¡ng PDF");
        }

        if (type == null || type.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LoÃƒÂ¡Ã‚ÂºÃ‚Â¡i tÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u lÃƒÆ’Ã‚Â  bÃƒÂ¡Ã‚ÂºÃ‚Â¯t buÃƒÂ¡Ã‚Â»Ã¢â€žÂ¢c");
        }

        DocumentType docType;
        try {
            docType = DocumentType.valueOf(type.strip());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LoÃƒÂ¡Ã‚ÂºÃ‚Â¡i tÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u khÃƒÆ’Ã‚Â´ng hÃƒÂ¡Ã‚Â»Ã‚Â£p lÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡: '" + type + "'");
        }

        InternDocumentResponse resp = documentService.uploadForIntern(targetInternId, docType, file, userId);
        return ResponseEntity.ok(ApiResponse.success("TÃƒÂ¡Ã‚ÂºÃ‚Â£i lÃƒÆ’Ã‚Âªn tÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng", resp));
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u khÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“n tÃƒÂ¡Ã‚ÂºÃ‚Â¡i: " + documentId));

        String fileUrl = doc.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u khÃƒÆ’Ã‚Â´ng cÃƒÆ’Ã‚Â³ Ãƒâ€žÃ¢â‚¬ËœÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Âng dÃƒÂ¡Ã‚ÂºÃ‚Â«n file");
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u khÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“n tÃƒÂ¡Ã‚ÂºÃ‚Â¡i: " + documentId));

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
        return ResponseEntity.ok(ApiResponse.success("PhÃƒÆ’Ã‚Âª duyÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡t tÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng", resp));
    }

    @PostMapping("/hr/documents/{id}/reject")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<InternDocumentResponse>> rejectDocument(
            @PathVariable("id") Long documentId,
            @RequestParam(value = "note", required = false) String note) {
        User user = currentUserOrThrow();
        InternDocumentResponse resp = documentService.reject(documentId, user.getId(), note);
        return ResponseEntity.ok(ApiResponse.success("TÃƒÂ¡Ã‚Â»Ã‚Â« chÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœi tÃƒÆ’Ã‚Â i liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng", resp));
    }

    @PostMapping(value = "/hr/interns/{internId}/documents/contracts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InternDocumentResponse>> uploadInternshipContract(
            @PathVariable("internId") Long internId,
            @RequestParam("file") MultipartFile file) {
        internProfileRepository.findById(internId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "KhÃƒÆ’Ã‚Â´ng tÃƒÆ’Ã‚Â¬m thÃƒÂ¡Ã‚ÂºÃ‚Â¥y hÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ sÃƒâ€ Ã‚Â¡ thÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â­p sinh: " + internId));
        User user = currentUserOrThrow();
        InternDocumentResponse resp = documentService.uploadForIntern(
                internId,
                DocumentType.INTERNSHIP_CONTRACT,
                file,
                user.getId());

        return ResponseEntity.ok(ApiResponse.success("TÃƒÂ¡Ã‚ÂºÃ‚Â£i lÃƒÆ’Ã‚Âªn hÃƒÂ¡Ã‚Â»Ã‚Â£p Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ng thÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â­p thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng", resp));
    }

    @PostMapping("/intern/documents/{id}/confirm")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<InternDocumentResponse>> confirmMyContract(@PathVariable("id") Long documentId) {
        User user = currentUserOrThrow();
        InternProfile ip = internProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "KhÃƒÆ’Ã‚Â´ng tÃƒÆ’Ã‚Â¬m thÃƒÂ¡Ã‚ÂºÃ‚Â¥y hÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ sÃƒâ€ Ã‚Â¡ thÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â­p sinh"));

        InternDocumentResponse resp = documentService.confirmContract(ip.getId(), documentId);
        return ResponseEntity.ok(ApiResponse.success("XÃƒÆ’Ã‚Â¡c nhÃƒÂ¡Ã‚ÂºÃ‚Â­n hÃƒÂ¡Ã‚Â»Ã‚Â£p Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ng thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng", resp));
    }
}

