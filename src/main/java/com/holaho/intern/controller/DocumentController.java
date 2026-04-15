package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.request.VerifyDocumentRequest;
import com.holaho.intern.shared.dto.response.DocumentResponse;
import com.holaho.intern.shared.dto.response.PageResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getMyDocuments(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<DocumentResponse> documents = documentService.getMyDocuments(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/intern/{internId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getInternDocuments(@PathVariable Long internId) {
        List<DocumentResponse> documents = documentService.getInternDocuments(internId);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<DocumentResponse>>> getPendingDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<DocumentResponse> documents = documentService.getPendingDocuments(page, size);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(documents)));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> verifyDocument(
            @PathVariable Long id,
            @Valid @RequestBody VerifyDocumentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        DocumentResponse response = documentService.verifyDocument(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Xác thực tài liệu thành công", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tài liệu thành công", null));
    }
}
