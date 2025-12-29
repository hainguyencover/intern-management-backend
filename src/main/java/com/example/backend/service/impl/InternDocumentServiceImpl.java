package com.example.backend.service.impl;

import com.example.backend.dto.StoredFile;
import com.example.backend.dto.response.InternDocumentResponse;
import com.example.backend.entity.InternDocument;
import com.example.backend.entity.InternProfile; // <-- đổi nếu tên khác
import com.example.backend.enums.DocumentType;
import com.example.backend.repository.InternDocumentRepository;
import com.example.backend.repository.InternProfileRepository; // <-- nếu chưa có thì tạo
import com.example.backend.service.InternDocumentService;
import com.example.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
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

        doc.setStatus("APPROVED");
        doc.setReviewedAt(LocalDateTime.now());
        // TODO: set reviewedBy = User(HR) via UserRepository if needed

        return toResponse(repo.save(doc));
    }

    @Override
    public InternDocumentResponse reject(Long documentId, Long hrUserId, String note) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        doc.setStatus("REJECTED");
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewNote(note);
        // TODO: set reviewedBy = User(HR)

        return toResponse(repo.save(doc));
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
                d.getReviewNote()
        );
    }
}
