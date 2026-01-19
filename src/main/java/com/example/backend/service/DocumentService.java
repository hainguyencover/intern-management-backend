package com.example.backend.service;

import com.example.backend.dto.request.VerifyDocumentRequest;
import com.example.backend.dto.response.DocumentResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final InternDocumentRepository documentRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<DocumentResponse> getByInternId(Long internId) {
        List<InternDocument> documents = documentRepository.findByInternId(internId);
        return documents.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getPending(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        return documentRepository.findByStatus("PENDING", pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + userId));

        List<InternDocument> documents = documentRepository.findByInternId(profile.getId());
        return documents.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getInternDocuments(Long internId) {
        if (!internProfileRepository.existsById(internId)) {
            throw new NotFoundException("Intern profile", internId);
        }

        List<InternDocument> documents = documentRepository.findByInternId(internId);
        return documents.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getPendingDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        // Use the custom query with JOIN FETCH
        Page<InternDocument> documents = documentRepository.findPendingDocuments(pageable);
        log.info("Found {} pending documents", documents.getTotalElements());
        return documents.map(this::mapToResponse);
    }

    @Transactional
    public DocumentResponse verifyDocument(Long documentId, VerifyDocumentRequest request, Long reviewerId) {
        InternDocument document = documentRepository.findByIdWithIntern(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        if (!"PENDING".equals(document.getStatus())) {
            throw new RuntimeException(
                    "Only PENDING documents can be verified. Current status: " + document.getStatus());
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found: " + reviewerId));

        document.setStatus(request.getDecision());
        document.setReviewNote(request.getNote());
        document.setReviewedBy(reviewer);
        document.setReviewedAt(LocalDateTime.now());

        document = documentRepository.save(document);
        log.info("Verified document {} with status: {}", documentId, request.getDecision());

        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id) {
        InternDocument document = documentRepository.findByIdWithIntern(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByInternId(Long internId) {
        return documentRepository.findByInternIdOrderByUploadedAtDesc(internId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentsByStatus(String status, Pageable pageable) {
        return documentRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void deleteDocument(Long id) {
        InternDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        // Delete physical file
        deleteFile(document.getFileUrl());

        documentRepository.delete(document);
        log.info("Deleted document: {}", id);
    }

    private void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && !fileUrl.isEmpty()) {
                String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadDir).resolve(filename);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", fileUrl, e);
        }
    }

    private DocumentResponse mapToResponse(InternDocument document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());

        // Handle potential null/corrupt data gracefully
        if (document.getIntern() != null) {
            response.setInternId(document.getIntern().getId());
            if (document.getIntern().getUser() != null) {
                response.setInternName(document.getIntern().getUser().getFullName());
            } else {
                response.setInternName("Unknown Intern (No User)");
            }
        } else {
            response.setInternId(null);
            response.setInternName("Unknown Intern");
        }

        response.setType(document.getType());
        response.setFileUrl(document.getFileUrl());
        response.setStatus(document.getStatus());
        response.setUploadedAt(document.getUploadedAt());

        if (document.getReviewedBy() != null) {
            response.setReviewedBy(document.getReviewedBy().getId());
            response.setReviewerName(document.getReviewedBy().getFullName());
            response.setReviewedAt(document.getReviewedAt());
            response.setReviewNote(document.getReviewNote());
        }

        return response;
    }
}
