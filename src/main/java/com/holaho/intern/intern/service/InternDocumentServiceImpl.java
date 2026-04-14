package com.holaho.intern.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.NotificationType;


import com.holaho.intern.shared.dto.StoredFile;
import com.holaho.intern.shared.dto.response.InternDocumentResponse;
import com.holaho.intern.intern.entity.InternDocument;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.enums.DocumentType;
import com.holaho.intern.shared.exception.ApiException;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.intern.service.InternDocumentService;
import com.holaho.intern.service.StorageService;
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
    private final com.holaho.intern.notification.service.NotificationService notificationService;
    private final com.holaho.intern.repository.ApplicationRepository appRepo;

    @Override
    public InternDocumentResponse uploadForIntern(Long internId, DocumentType type, MultipartFile file,
            Long uploaderId) {
        InternProfile intern = internRepo.findById(internId)
                .orElseThrow(() -> new RuntimeException("Intern not found: " + internId));

        // lÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â°u file via storage service
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
            com.holaho.intern.user.entity.User uploader = userRepository.findById(uploaderId)
                    .orElseThrow(() -> new RuntimeException("Uploader not found"));
            doc.setReviewedBy(uploader);
            doc.setReviewedAt(LocalDateTime.now());
            doc.setReviewNote("Uploaded by HR/Admin");

            // Update Application Status to CONTRACT_SENT if it's a contract
            if (DocumentType.INTERNSHIP_CONTRACT == type) {
                updateApplicationStatus(internId, com.holaho.intern.shared.enums.ApplicationStatus.CONTRACT_SENT);
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

        com.holaho.intern.user.entity.User hr = userRepository.findById(hrUserId)
                .orElseThrow(() -> new RuntimeException("HR User not found: " + hrUserId));

        doc.setStatus("APPROVED");
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewedBy(hr);

        InternDocument saved = repo.save(doc);

        // Notify Intern
        notificationService.createNotification(
                doc.getIntern().getUser().getId(),
                com.holaho.intern.shared.enums.NotificationType.APPLICATION,
                "TÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â i liÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¡u ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¹Ã…â€œÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â°ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£c duyÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¡t",
                "TÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â i liÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¡u " + doc.getType() + " cÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§a bÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡n ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¹Ã…â€œÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£ ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¹Ã…â€œÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â°ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£c chÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¥p thuÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­n.");

        return toResponse(saved);
    }

    @Override
    public InternDocumentResponse reject(Long documentId, Long hrUserId, String note) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        com.holaho.intern.user.entity.User hr = userRepository.findById(hrUserId)
                .orElseThrow(() -> new RuntimeException("HR User not found: " + hrUserId));

        doc.setStatus("REJECTED");
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewNote(note);
        doc.setReviewedBy(hr);

        InternDocument saved = repo.save(doc);

        // Notify Intern
        notificationService.createNotification(
                doc.getIntern().getUser().getId(),
                com.holaho.intern.shared.enums.NotificationType.APPLICATION,
                "TÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â i liÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¡u bÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¹ tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â« chÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¹Ã…â€œi",
                "TÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â i liÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¡u " + doc.getType() + " bÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¹ tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â« chÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¹Ã…â€œi. LÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â½ do: " + note);

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

        // Intern chÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â° ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¹Ã…â€œÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â°ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£c xÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡c nhÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­n sau khi HR ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¹Ã…â€œÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£ duyÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¡t hÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£p ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¹Ã…â€œÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“ng (chÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¥p nhÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­n cÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£ APPROVED
        // vÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â  APPROVE do lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¹ch sÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­ dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¯ liÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¡u)
        String st = doc.getStatus();
        if (!"APPROVED".equalsIgnoreCase(st) && !"APPROVE".equalsIgnoreCase(st)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contract must be APPROVED before confirming");
        }

        doc.setStatus("SIGNED");
        InternDocument saved = repo.save(doc);

        updateApplicationStatus(internId, com.holaho.intern.shared.enums.ApplicationStatus.CONTRACT_SIGNED);

        return toResponse(saved);
    }

    private void updateApplicationStatus(Long internId, com.holaho.intern.shared.enums.ApplicationStatus newStatus) {
        List<com.holaho.intern.entity.Application> apps = appRepo.findByIntern_Id(internId);
        if (!apps.isEmpty()) {
            // Find latest app
            apps.sort((a1, a2) -> {
                if (a1.getAppliedAt() == null || a2.getAppliedAt() == null)
                    return 0;
                return a2.getAppliedAt().compareTo(a1.getAppliedAt());
            });
            com.holaho.intern.entity.Application latestApp = apps.get(0);

            boolean canUpdate = false;
            com.holaho.intern.shared.enums.ApplicationStatus current = latestApp.getStatus();

            if (newStatus == com.holaho.intern.shared.enums.ApplicationStatus.CONTRACT_SENT) {
                if (current == com.holaho.intern.shared.enums.ApplicationStatus.APPROVED)
                    canUpdate = true;
            } else if (newStatus == com.holaho.intern.shared.enums.ApplicationStatus.CONTRACT_SIGNED) {
                if (current == com.holaho.intern.shared.enums.ApplicationStatus.CONTRACT_SENT ||
                        current == com.holaho.intern.shared.enums.ApplicationStatus.APPROVED)
                    canUpdate = true;
            }

            if (canUpdate) {
                latestApp.setStatus(newStatus);
                appRepo.save(latestApp);
            }
        }
    }
}

