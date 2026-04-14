package com.holaho.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.intern.entity.InternDocument;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.exception.NotFoundException;


import com.holaho.intern.shared.dto.request.VerifyDocumentRequest;
import com.holaho.intern.shared.dto.response.DocumentResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
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
    private final ApplicationRepository applicationRepository;

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

        // US10 Fix: Update Application Status and Intern Status based on Document
        // Approval
        String decision = request.getDecision();
        boolean isApproved = "APPROVE".equalsIgnoreCase(decision) || "APPROVED".equalsIgnoreCase(decision);

        if (isApproved) {
            List<Application> apps = applicationRepository.findByIntern_Id(document.getIntern().getId());
            if (!apps.isEmpty()) {
                // Find latest app
                apps.sort((a1, a2) -> {
                    if (a1.getAppliedAt() == null || a2.getAppliedAt() == null)
                        return 0;
                    return a2.getAppliedAt().compareTo(a1.getAppliedAt());
                });
                Application latestApp = apps.get(0);

                // Case 1: CV or Letter Approved -> Application APPROVED
                if ("CV".equals(document.getType()) || "APPLICATION_LETTER".equals(document.getType())) {
                    if (latestApp.getStatus() == ApplicationStatus.SUBMITTED ||
                            latestApp.getStatus() == ApplicationStatus.DRAFT) { // Maybe Draft too?

                        latestApp.setStatus(ApplicationStatus.APPROVED);
                        applicationRepository.save(latestApp);
                        log.info("Auto-updated Application {} status to APPROVED", latestApp.getId());
                    }
                }

                // Case 2: Contract Approved -> Application CONTRACT_SIGNED
                if ("INTERNSHIP_CONTRACT".equals(document.getType()) || "CONTRACT".equals(document.getType())) {
                    if (latestApp.getStatus() == ApplicationStatus.APPROVED ||
                            latestApp.getStatus() == ApplicationStatus.CONTRACT_SENT) {

                        latestApp.setStatus(ApplicationStatus.CONTRACT_SIGNED);
                        applicationRepository.save(latestApp);
                        log.info("Auto-updated Application {} status to CONTRACT_SIGNED", latestApp.getId());
                    }
                }
            }
        }

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

