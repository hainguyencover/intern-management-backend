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
    private final com.example.backend.repository.ApplicationRepository appRepo;

    @Override
    public InternDocumentResponse uploadForIntern(Long internId, DocumentType type, MultipartFile file,
            Long uploaderId) {
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
        doc.setUploadedAt(LocalDateTime.now());

        // Determine status based on uploader
        boolean isSelfUpload = intern.getUser().getId().equals(uploaderId);

        if (isSelfUpload) {
            doc.setStatus("PENDING");
            // reset review fields
            doc.setReviewedBy(null);
            doc.setReviewedAt(null);
            doc.setReviewNote(null);
        } else {
            // HR/Admin upload -> Auto Approve
            doc.setStatus("APPROVED");
            com.example.backend.entity.User uploader = userRepository.findById(uploaderId)
                    .orElseThrow(() -> new RuntimeException("Uploader not found"));
            doc.setReviewedBy(uploader);
            doc.setReviewedAt(LocalDateTime.now());
            doc.setReviewNote("Uploaded by HR/Admin");

            // Update Application Status to CONTRACT_SENT if it's a contract
            if (DocumentType.INTERNSHIP_CONTRACT == type) {
                updateApplicationStatus(internId, com.example.backend.enums.ApplicationStatus.CONTRACT_SENT);
            }
        }

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

        // Intern chỉ được xác nhận sau khi HR đã duyệt hợp đồng (chấp nhận cả APPROVED
        // và APPROVE do lịch sử dữ liệu)
        String st = doc.getStatus();
        if (!"APPROVED".equalsIgnoreCase(st) && !"APPROVE".equalsIgnoreCase(st)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contract must be APPROVED before confirming");
        }

        doc.setStatus("SIGNED");
        InternDocument saved = repo.save(doc);

        updateApplicationStatus(internId, com.example.backend.enums.ApplicationStatus.CONTRACT_SIGNED);

        return toResponse(saved);
    }

    private void updateApplicationStatus(Long internId, com.example.backend.enums.ApplicationStatus newStatus) {
        List<com.example.backend.entity.Application> apps = appRepo.findByIntern_Id(internId);
        if (!apps.isEmpty()) {
            // Find latest app
            apps.sort((a1, a2) -> {
                if (a1.getAppliedAt() == null || a2.getAppliedAt() == null)
                    return 0;
                return a2.getAppliedAt().compareTo(a1.getAppliedAt());
            });
            com.example.backend.entity.Application latestApp = apps.get(0);

            boolean canUpdate = false;
            com.example.backend.enums.ApplicationStatus current = latestApp.getStatus();

            if (newStatus == com.example.backend.enums.ApplicationStatus.CONTRACT_SENT) {
                if (current == com.example.backend.enums.ApplicationStatus.APPROVED)
                    canUpdate = true;
            } else if (newStatus == com.example.backend.enums.ApplicationStatus.CONTRACT_SIGNED) {
                if (current == com.example.backend.enums.ApplicationStatus.CONTRACT_SENT ||
                        current == com.example.backend.enums.ApplicationStatus.APPROVED)
                    canUpdate = true;
            }

            if (canUpdate) {
                latestApp.setStatus(newStatus);
                appRepo.save(latestApp);
            }
        }
    }
}
