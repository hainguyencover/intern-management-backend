package com.example.backend.controller;

import com.example.backend.dto.response.InternDocumentResponse;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.service.InternDocumentService;
import com.example.backend.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.backend.enums.DocumentType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InternDocumentController {

    private final InternDocumentService documentService;
    private final UserRepository userRepository;
    private final com.example.backend.repository.InternDocumentRepository internDocumentRepository;
    private final InternProfileRepository internProfileRepository;
    private final StorageService storage;

    @GetMapping("/intern/documents")
    public ResponseEntity<?> getMyDocuments() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String principalName = auth.getName(); // usually email
        User user = userRepository.findByEmail(principalName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principalName));

        Long userId = user.getId();

        // Ensure only users with INTERN role can access their documents.
        // Use a repository-level query to avoid triggering lazy-loading of `roles`.
        boolean isIntern = userRepository.existsByIdAndRoleCode(userId, "INTERN");
        if (!isIntern) {
            // If the user is HR or ADMIN, redirect them to the HR interns documents path
            // (no internId available).
            boolean isHrOrAdmin = userRepository.existsByIdAndRoleCode(userId, "HR")
                    || userRepository.existsByIdAndRoleCode(userId, "ADMIN");
            if (isHrOrAdmin) {
                return ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .header("Location", "/api/hr/interns")
                        .body(java.util.Map.of(
                                "message", "Redirecting HR/Admin to /api/hr/interns to select an intern.",
                                "redirect", "/api/hr/interns"));
            }

            throw new ApiException(HttpStatus.FORBIDDEN,
                    "User '" + principalName + "' does not have INTERN role and cannot access this endpoint. " +
                            "If you are HR/Admin and want to view an intern's documents, use GET /api/hr/interns/{internId}/documents.");
        }

        var optProfile = internProfileRepository.findByUser_Id(userId);
        var ip = optProfile.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Intern profile not found for user '" + principalName + "' (userId=" + userId
                        + "). Please contact HR."));

        Long internId = ip.getId();
        List<InternDocumentResponse> docs = documentService.getMyDocuments(internId);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/hr/interns/{id}/documents")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<List<InternDocumentResponse>> getInternDocumentsForHr(@PathVariable("id") Long internId) {
        List<InternDocumentResponse> docs = documentService.getDocumentsOfIntern(internId);
        return ResponseEntity.ok(docs);
    }

    @PostMapping(path = {"/intern/documents", "/intern/documents/upload", "/documents/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InternDocumentResponse> uploadMyDocument(
            @RequestParam("type") String type,
            @RequestParam("file") MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String principalName = auth.getName();
        User user = userRepository.findByEmail(principalName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principalName));

        Long userId = user.getId();
        boolean isIntern = userRepository.existsByIdAndRoleCode(userId, "INTERN");
        if (!isIntern) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "User '" + principalName + "' does not have INTERN role and cannot upload documents.");
        }

        var optProfile = internProfileRepository.findByUser_Id(userId);
        var ip = optProfile.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Intern profile not found for user '" + principalName + "' (userId=" + userId
                        + "). Please contact HR."));

        DocumentType docType;
        try {
            docType = DocumentType.valueOf(type.strip());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid document type: '" + type + "'");
        }

        InternDocumentResponse resp = documentService.uploadForIntern(ip.getId(), docType, file);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("id") Long documentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String principalName = auth.getName();
        com.example.backend.entity.User user = userRepository.findByEmail(principalName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principalName));

        Long userId = user.getId();
        boolean isHr = userRepository.existsByIdAndRoleCode(userId, "HR");

        var doc = internDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        String fileUrl = doc.getFileUrl();
        String filename = doc.getFileUrl() != null ? doc.getFileUrl().substring(doc.getFileUrl().lastIndexOf('/') + 1)
                : "file";

        // If requester is HR, allow download only for PDF files
        boolean isPdf = filename.toLowerCase().endsWith(".pdf");
        if (isHr && !isPdf) {
            throw new ApiException(HttpStatus.NOT_ACCEPTABLE, "HR can download only PDF files");
        }

        Resource resource = storage.loadFileAsResource(fileUrl);

        try {
            long size = resource.contentLength();
            MediaType contentType = isPdf ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
            if (isHr && !isPdf) {
                throw new ApiException(HttpStatus.NOT_ACCEPTABLE, "HR can download only PDF files");
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentLength(size)
                    .contentType(contentType)
                    .body(resource);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to prepare file for download");
        }
    }

    @GetMapping("/documents/download/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDocumentAlias(
            @PathVariable("id") Long documentId) {
        // alias to support frontend path /api/documents/download/{id}
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String principalName = auth.getName();
        com.example.backend.entity.User user = userRepository.findByEmail(principalName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principalName));

        Long userId = user.getId();
        boolean isHr = userRepository.existsByIdAndRoleCode(userId, "HR");

        // reuse same logic as main download endpoint
        return downloadDocument(documentId);
    }

    @GetMapping("/documents/{id}/status")
    public ResponseEntity<?> getDocumentStatus(@PathVariable("id") Long documentId) {
        var doc = internDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        return ResponseEntity.ok(java.util.Map.of(
                "id", doc.getId(),
                "status", doc.getStatus(),
                "reviewedById", doc.getReviewedBy() != null ? doc.getReviewedBy().getId() : null,
                "reviewedAt", doc.getReviewedAt(),
                "reviewNote", doc.getReviewNote()));
    }

    @PostMapping("/hr/documents/{id}/approve")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternDocumentResponse> approveDocument(@PathVariable("id") Long documentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principalName = auth.getName();
        com.example.backend.entity.User user = userRepository.findByEmail(principalName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principalName));

        InternDocumentResponse resp = documentService.approve(documentId, user.getId());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/hr/documents/{id}/reject")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternDocumentResponse> rejectDocument(@PathVariable("id") Long documentId,
                                                                 @RequestParam(value = "note", required = false) String note) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principalName = auth.getName();
        com.example.backend.entity.User user = userRepository.findByEmail(principalName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principalName));

        InternDocumentResponse resp = documentService.reject(documentId, user.getId(), note);
        return ResponseEntity.ok(resp);
    }

    // Support legacy frontend PUT URLs: /api/documents/{id}/approve and
    // /api/documents/{id}/reject
    @PutMapping("/documents/{id}/approve")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternDocumentResponse> approveDocumentPut(@PathVariable("id") Long documentId,
                                                                     @RequestParam(value = "hrUserId", required = false) Long hrUserId) {
        Long actingHrId = hrUserId;
        if (actingHrId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String principalName = auth.getName();
            com.example.backend.entity.User user = userRepository.findByEmail(principalName)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principalName));
            actingHrId = user.getId();
        }

        InternDocumentResponse resp = documentService.approve(documentId, actingHrId);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/documents/{id}/reject")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternDocumentResponse> rejectDocumentPut(@PathVariable("id") Long documentId,
                                                                    @RequestParam(value = "hrUserId", required = false) Long hrUserId,
                                                                    @RequestParam(value = "note", required = false) String note) {
        Long actingHrId = hrUserId;
        if (actingHrId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String principalName = auth.getName();
            com.example.backend.entity.User user = userRepository.findByEmail(principalName)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principalName));
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
