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
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.intern.service.InternDocumentService;
import com.holaho.intern.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
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

    @Override
    public InternDocumentResponse uploadForIntern(Long internId, DocumentType type, MultipartFile file,
            Long uploaderId) {
        InternProfile intern = internRepo.findById(internId)
                .orElseThrow(() -> new NotFoundException("Thực tập sinh không tồn tại: " + internId));

        // Lưu file via storage service
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
            User uploader = userRepository.findById(uploaderId)
                    .orElseThrow(() -> new NotFoundException("Người tải lên không tồn tại: " + uploaderId));
            doc.setReviewedBy(uploader);
            doc.setReviewedAt(LocalDateTime.now());
            doc.setReviewNote("Uploaded by HR/Admin");

            // Update Application Status to CONTRACT_SENT if it's a contract
            if (DocumentType.INTERNSHIP_CONTRACT == type) {
                updateApplicationStatus(internId, ApplicationStatus.CONTRACT_SENT);
            }
        }

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
                .orElseThrow(() -> new NotFoundException("Tài liệu không tồn tại: " + documentId));

        User hr = userRepository.findById(hrUserId)
                .orElseThrow(() -> new NotFoundException("HR User không tồn tại: " + hrUserId));

        doc.setStatus("APPROVED");
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewedBy(hr);

        InternDocument saved = repo.save(doc);

        // Notify Intern
        notificationService.createNotification(
                doc.getIntern().getUser().getId(),
                NotificationType.APPLICATION,
                "Tài liệu được duyệt",
                "Tài liệu " + doc.getType() + " của bạn đã được chấp thuận.");

        return toResponse(saved);
    }

    @Override
    public InternDocumentResponse reject(Long documentId, Long hrUserId, String note) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Tài liệu không tồn tại: " + documentId));

        User hr = userRepository.findById(hrUserId)
                .orElseThrow(() -> new NotFoundException("HR User không tồn tại: " + hrUserId));

        doc.setStatus("REJECTED");
        doc.setReviewedAt(LocalDateTime.now());
        doc.setReviewNote(note);
        doc.setReviewedBy(hr);

        InternDocument saved = repo.save(doc);

        // Notify Intern
        notificationService.createNotification(
                doc.getIntern().getUser().getId(),
                NotificationType.APPLICATION,
                "Tài liệu bị từ chối",
                "Tài liệu " + doc.getType() + " bị từ chối. Lý do: " + note);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public StoredFile download(Long documentId, Long requesterUserId, boolean isHr) {
        InternDocument doc = repo.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Tài liệu không tồn tại: " + documentId));

        // Permission check: requester must be the document owner or HR
        if (!isHr) {
            Long ownerUserId = doc.getIntern().getUser().getId();
            if (!ownerUserId.equals(requesterUserId)) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Bạn không có quyền tải tài liệu của thực tập sinh khác");
            }
        }
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

        // Intern chỉ được xác nhận sau khi HR đã duyệt hợp đồng (status APPROVED)
        String st = doc.getStatus();
        if (!"APPROVED".equalsIgnoreCase(st) && !"APPROVE".equalsIgnoreCase(st)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Hợp đồng cần được duyệt trước khi xác nhận");
        }

        doc.setStatus("SIGNED");
        InternDocument saved = repo.save(doc);

        updateApplicationStatus(internId, ApplicationStatus.CONTRACT_SIGNED);

        return toResponse(saved);
    }

    private void updateApplicationStatus(Long internId, ApplicationStatus newStatus) {
        List<Application> apps = appRepo.findByIntern_Id(internId);
        if (!apps.isEmpty()) {
            // Find latest app
            apps.sort((a1, a2) -> {
                if (a1.getAppliedAt() == null || a2.getAppliedAt() == null)
                    return 0;
                return a2.getAppliedAt().compareTo(a1.getAppliedAt());
            });
            Application latestApp = apps.get(0);

            boolean canUpdate = false;
            ApplicationStatus current = latestApp.getStatus();

            if (newStatus == ApplicationStatus.CONTRACT_SENT) {
                if (current == ApplicationStatus.APPROVED)
                    canUpdate = true;
            } else if (newStatus == ApplicationStatus.CONTRACT_SIGNED) {
                if (current == ApplicationStatus.CONTRACT_SENT ||
                        current == ApplicationStatus.APPROVED)
                    canUpdate = true;
            }

            if (canUpdate) {
                latestApp.setStatus(newStatus);
                appRepo.save(latestApp);
            }
        }
    }
}
