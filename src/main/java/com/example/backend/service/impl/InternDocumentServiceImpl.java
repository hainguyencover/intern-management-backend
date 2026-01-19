package com.example.backend.service.impl;

import com.example.backend.dto.StoredFile;
import com.example.backend.dto.response.InternDocumentResponse;
import com.example.backend.entity.InternDocument;
import com.example.backend.entity.InternProfile;
import com.example.backend.enums.DocumentType;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.InternDocumentRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.InternDocumentService;
import com.example.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InternDocumentServiceImpl implements InternDocumentService {

    private final InternDocumentRepository repo;
    private final InternProfileRepository internRepo;
    private final StorageService storage;
    private final UserRepository userRepository;
    private final com.example.backend.service.NotificationService notificationService;

    @Override
    public InternDocumentResponse uploadForIntern(Long internId, DocumentType type, MultipartFile file) {
        InternProfile intern = internRepo.findById(internId)
                .orElseThrow(() -> new RuntimeException("Intern not found: " + internId));

        // lưu file via storage service
        StoredFile stored = storage.saveInternDocument(internId, type, file);

        // Rule: update latest record of same type or create new
        InternDocument doc = repo.findTopByIntern_IdAndTypeOrderByUploadedAtDesc(internId, type.name())
                .orElseGet(InternDocument::new);

        doc.setIntern(intern);
        doc.setType(type.name());
        doc.setFileUrl(stored.fileUrl());
        doc.setStatus("PENDING");
        doc.setUploadedAt(LocalDateTime.now());

        // reset review fields
        doc.setReviewedBy(null);
        doc.setReviewedAt(null);
        doc.setReviewNote(null);

        // Notify HR (optional, but good for workflow)
        // notificationService.createNotification(...);

        return toResponse(repo.save(doc));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternDocumentResponse> getMyDocuments(Long internId) {
        return repo.findByIntern_IdOrderByUploadedAtDesc(internId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternDocumentResponse> getDocumentsOfIntern(Long internId) {
        return repo.findByIntern_IdOrderByUploadedAtDesc(internId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public InternDocumentResponse approve(Long documentId, Long hrUserId) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        com.example.backend.entity.User hr = userRepository.findById(hrUserId)
                .orElseThrow(() -> new RuntimeException("HR User not found: " + hrUserId));

        doc.setStatus("APPROVED");
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewedBy(hr);

        InternDocument saved = repo.save(doc);

        // Notify Intern
        notificationService.createNotification(
                doc.getIntern().getUser().getId(),
                com.example.backend.enums.NotificationType.APPLICATION,
                "Tài liệu được duyệt",
                "Tài liệu " + doc.getType() + " của bạn đã được chấp thuận.");

        return toResponse(saved);
    }

    @Override
    public InternDocumentResponse reject(Long documentId, Long hrUserId, String note) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        com.example.backend.entity.User hr = userRepository.findById(hrUserId)
                .orElseThrow(() -> new RuntimeException("HR User not found: " + hrUserId));

        doc.setStatus("REJECTED");
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewNote(note);
        doc.setReviewedBy(hr);

        InternDocument saved = repo.save(doc);

        // Notify Intern
        notificationService.createNotification(
                doc.getIntern().getUser().getId(),
                com.example.backend.enums.NotificationType.APPLICATION,
                "Tài liệu bị từ chối",
                "Tài liệu " + doc.getType() + " bị từ chối. Lý do: " + note);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public StoredFile download(Long documentId, Long requesterUserId, boolean isHr) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // TODO: permission checks
        return storage.loadAsResource(doc.getFileUrl());
    }

    private InternDocumentResponse toResponse(InternDocument d) {
        Long internId = (d.getIntern() != null ? d.getIntern().getId() : null);
        Long reviewedById = (d.getReviewedBy() != null ? d.getReviewedBy().getId() : null);

        return new InternDocumentResponse(
                d.getId(),
                internId,
                d.getType(),
                d.getFileUrl(),
                d.getStatus(),
                d.getUploadedAt(),
                reviewedById,
                d.getReviewedAt(),
                d.getReviewNote());
    }

    @Override
    public InternDocumentResponse confirmContract(Long internId, Long documentId) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found: " + documentId));

        if (doc.getIntern() == null || !doc.getIntern().getId().equals(internId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot confirm other intern's document");
        }

        if (!DocumentType.INTERNSHIP_CONTRACT.name().equals(doc.getType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only internship contract can be confirmed");
        }

        if ("SIGNED".equalsIgnoreCase(doc.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contract is already signed");
        }

        // Intern chỉ được xác nhận sau khi HR đã duyệt hợp đồng
        if (!"APPROVED".equalsIgnoreCase(doc.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contract must be APPROVED before confirming");
        }

        doc.setStatus("SIGNED");

        return toResponse(repo.save(doc));
    }
}
