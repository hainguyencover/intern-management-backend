package com.holaho.intern.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.intern.entity.InternDocument;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.service.DocumentValidationService;
import com.holaho.intern.service.StorageService;
import com.holaho.intern.shared.dto.StoredFile;
import com.holaho.intern.shared.dto.response.InternDocumentResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.DocumentType;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.ApiException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InternDocumentServiceImpl implements InternDocumentService {

    private final InternDocumentRepository repo;
    private final InternProfileRepository internRepo;
    private final StorageService storage;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationRepository appRepo;
    private final DocumentValidationService validationService;

    @Override
    public InternDocumentResponse uploadForIntern(Long internId, DocumentType type, MultipartFile file, Long uploaderId) {
        // 1. Validation
        validationService.validateDocumentFile(file);

        InternProfile intern = internRepo.findById(internId)
                .orElseThrow(() -> new NotFoundException("Thực tập sinh không tồn tại: " + internId));

        // 2. Calculate Checksum (SHA-256)
        String checksum = calculateChecksum(file);

        // 3. Store file via storage service
        StoredFile stored = storage.saveInternDocument(internId, type, file);

        try {
            // Find existing record of same type to update or create new version
            InternDocument doc = repo.findTopByIntern_IdAndTypeOrderByUploadedAtDesc(internId, type.name())
                    .orElseGet(InternDocument::new);

            doc.setIntern(intern);
            doc.setType(type.name());
            doc.setFileUrl(stored.fileUrl());
            doc.setOriginalFileName(file.getOriginalFilename());
            doc.setStoredFileName(stored.filename());
            doc.setStorageKey("tenants/" + intern.getTenantId() + "/interns/" + internId + "/documents/" + type.name() + "/" + stored.filename());
            doc.setContentType(file.getContentType());
            doc.setFileSize(file.getSize());
            doc.setChecksum(checksum);
            doc.setUploadedAt(LocalDateTime.now());
            if (intern.getTenantId() != null) {
                doc.setTenantId(intern.getTenantId());
            }

            boolean isSelfUpload = intern.getUser() != null && intern.getUser().getId().equals(uploaderId);

            if (isSelfUpload) {
                doc.setStatus("PENDING");
                doc.setReviewedBy(null);
                doc.setReviewedAt(null);
                doc.setReviewNote(null);
                doc.setRejectionReason(null);
            } else {
                // HR/Admin upload -> Auto Approve
                doc.setStatus("APPROVED");
                User uploader = userRepository.findById(uploaderId)
                        .orElseThrow(() -> new NotFoundException("Người tải lên không tồn tại: " + uploaderId));
                doc.setReviewedBy(uploader);
                doc.setReviewedAt(LocalDateTime.now());
                doc.setReviewNote("Uploaded & Auto-Approved by HR/Admin");
                doc.setRejectionReason(null);

                if (DocumentType.INTERNSHIP_CONTRACT == type) {
                    updateApplicationStatus(internId, ApplicationStatus.CONTRACT_SENT);
                }
            }

            InternDocument savedDoc = repo.save(doc);
            log.info("Document ID {} ({}) uploaded successfully for Intern ID {}", savedDoc.getId(), type.name(), internId);
            return toResponse(savedDoc);

        } catch (Exception e) {
            log.error("Failed to save document metadata for internId {}. Cleaning up stored file: {}", internId, stored.fileUrl(), e);
            try {
                storage .deleteFile(stored.fileUrl());
            } catch (Exception cleanupEx) {
                log.warn("Storage cleanup failed for URL: {}", stored.fileUrl(), cleanupEx);
            }
            throw e;
        }
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
    @Transactional(readOnly = true)
    public Page<InternDocumentResponse> getDocumentsForHr(String status, String documentType, Long tenantId, Pageable pageable) {
        return repo.findFilteredDocuments(tenantId, status, documentType, pageable)
                .map(this::toResponse);
    }

    @Override
    public InternDocumentResponse approve(Long documentId, Long hrUserId) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Tài liệu không tồn tại: " + documentId));

        User hr = userRepository.findById(hrUserId)
                .orElseThrow(() -> new NotFoundException("HR User không tồn tại: " + hrUserId));

        doc.setStatus("APPROVED");
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewedBy(hr);
        doc.setRejectionReason(null);

        InternDocument saved = repo.save(doc);

        if (doc.getIntern() != null && doc.getIntern().getUser() != null) {
            notificationService.createNotification(
                    doc.getIntern().getUser().getId(),
                    NotificationType.APPLICATION,
                    "Tài liệu đã được phê duyệt",
                    "Tài liệu " + doc.getType() + " của bạn đã được duyệt thành công.");
        }

        return toResponse(saved);
    }

    @Override
    public InternDocumentResponse reject(Long documentId, Long hrUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Lý do từ chối không được để trống (rejectionReason required).");
        }

        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Tài liệu không tồn tại: " + documentId));

        User hr = userRepository.findById(hrUserId)
                .orElseThrow(() -> new NotFoundException("HR User không tồn tại: " + hrUserId));

        doc.setStatus("REJECTED");
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewNote(reason.trim());
        doc.setRejectionReason(reason.trim());
        doc.setReviewedBy(hr);

        InternDocument saved = repo.save(doc);

        if (doc.getIntern() != null && doc.getIntern().getUser() != null) {
            notificationService.createNotification(
                    doc.getIntern().getUser().getId(),
                    NotificationType.APPLICATION,
                    "Tài liệu bị từ chối",
                    "Tài liệu " + doc.getType() + " bị từ chối với lý do: " + reason.trim());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile download(Long documentId, Long requesterUserId, boolean isHr) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Tài liệu không tồn tại: " + documentId));

        if (!isHr) {
            Long ownerUserId = doc.getIntern() != null && doc.getIntern().getUser() != null
                    ? doc.getIntern().getUser().getId()
                    : null;
            if (ownerUserId == null || !ownerUserId.equals(requesterUserId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập hoặc tải tài liệu này.");
            }
        }
        return storage.loadAsResource(doc.getFileUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAllRequiredDocuments(Long internId) {
        boolean cvApproved = repo.existsByIntern_IdAndTypeAndStatus(internId, DocumentType.CV.name(), "APPROVED");
        boolean appApproved = repo.existsByIntern_IdAndTypeAndStatus(internId, DocumentType.INTERNSHIP_APPLICATION.name(), "APPROVED")
                || repo.existsByIntern_IdAndTypeAndStatus(internId, DocumentType.APPLICATION_LETTER.name(), "APPROVED");
        return cvApproved && appApproved;
    }


    @Override
    public InternDocumentResponse confirmContract(Long internId, Long documentId) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài liệu không tồn tại: " + documentId));

        if (doc.getIntern() == null || !doc.getIntern().getId().equals(internId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không thể xác nhận tài liệu của thực tập sinh khác");
        }

        if (!DocumentType.INTERNSHIP_CONTRACT.name().equals(doc.getType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ hợp đồng thực tập mới có thể xác nhận");
        }

        if ("SIGNED".equalsIgnoreCase(doc.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Hợp đồng đã được ký");
        }

        String st = doc.getStatus();
        if (!"APPROVED".equalsIgnoreCase(st) && !"APPROVE".equalsIgnoreCase(st)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Hợp đồng cần được duyệt trước khi xác nhận");
        }

        doc.setStatus("SIGNED");
        InternDocument saved = repo.save(doc);

        updateApplicationStatus(internId, ApplicationStatus.CONTRACT_SIGNED);

        InternProfile profile = doc.getIntern();
        if (profile != null) {
            profile.setStatus("INTERNING");
            internRepo.save(profile);
            log.info("Transitioned InternProfile {} status to INTERNING upon contract signature", internId);
        }

        return toResponse(saved);
    }

    private void updateApplicationStatus(Long internId, ApplicationStatus newStatus) {
        List<Application> apps = appRepo.findByIntern_Id(internId);
        if (!apps.isEmpty()) {
            apps.sort((a1, a2) -> {
                if (a1.getAppliedAt() == null || a2.getAppliedAt() == null) return 0;
                return a2.getAppliedAt().compareTo(a1.getAppliedAt());
            });
            Application latestApp = apps.get(0);

            boolean canUpdate = false;
            ApplicationStatus current = latestApp.getStatus();

            if (newStatus == ApplicationStatus.CONTRACT_SENT) {
                if (current == ApplicationStatus.APPROVED) canUpdate = true;
            } else if (newStatus == ApplicationStatus.CONTRACT_SIGNED) {
                if (current == ApplicationStatus.CONTRACT_SENT || current == ApplicationStatus.APPROVED) canUpdate = true;
            }

            if (canUpdate) {
                latestApp.setStatus(newStatus);
                appRepo.save(latestApp);
            }
        }
    }

    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | java.io.IOException e) {
            log.warn("Unable to calculate SHA-256 checksum for file {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    private InternDocumentResponse toResponse(InternDocument d) {
        Long internId = (d.getIntern() != null ? d.getIntern().getId() : null);
        String internName = (d.getIntern() != null && d.getIntern().getUser() != null)
                ? d.getIntern().getUser().getFullName()
                : null;
        Long reviewedById = (d.getReviewedBy() != null ? d.getReviewedBy().getId() : null);
        String reviewedByName = (d.getReviewedBy() != null ? d.getReviewedBy().getFullName() : null);

        return new InternDocumentResponse(
                d.getId(),
                internId,
                internName,
                d.getType(),
                d.getOriginalFileName(),
                d.getFileUrl(),
                d.getFileSize(),
                d.getContentType(),
                d.getStatus(),
                d.getUploadedAt(),
                reviewedById,
                reviewedByName,
                d.getReviewedAt(),
                d.getReviewNote(),
                d.getRejectionReason()
        );
    }
}
