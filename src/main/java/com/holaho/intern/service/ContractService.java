package com.holaho.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.intern.entity.InternshipContract;
import com.holaho.intern.intern.repository.InternshipContractRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.shared.dto.request.ContractRevisionRequest;
import com.holaho.intern.shared.dto.response.ContractResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.ContractStatus;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.ForbiddenException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.workflow.ApplicationStateMachine;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private final InternshipContractRepository contractRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final ApplicationStateMachine stateMachine;
    private final AuditLogService auditLogService;

    /**
     * US-009 / US-048: HR uploads or replaces internship contract PDF.
     */
    @Transactional
    public ContractResponse uploadContract(Long applicationId, MultipartFile file, Long hrUserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application", applicationId));

        // Validation 1: Application must not be REJECTED
        if (application.getStatus() == ApplicationStatus.REJECTED) {
            throw new BadRequestException("Không thể gửi hợp đồng cho đơn ứng tuyển đã bị từ chối (REJECTED)");
        }

        // Retrieve existing or create new contract
        InternshipContract contract = contractRepository.findByApplication_Id(applicationId)
                .orElseGet(InternshipContract::new);

        // Validation 2: Immutability Check - Locked if already SIGNED
        boolean isReplacement = (contract.getId() != null);
        if (isReplacement && contract.getStatus() == ContractStatus.SIGNED) {
            throw new ConflictException("Hợp đồng này đã được các bên ký hoàn tất (SIGNED). Không thể thay thế file hợp đồng mới.");
        }

        // Validation 3: File must not be null or empty
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File hợp đồng không được để trống");
        }

        // Validation 4: File must be PDF
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Hợp đồng thực tập bắt buộc phải ở định dạng file PDF (.pdf)");
        }

        // Validation 5: File size limit (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("Dung lượng file hợp đồng vượt quá giới hạn tối đa 10MB");
        }

        // Calculate SHA-256 checksum for audit evidence
        String docHash = calculateSha256(file);

        // Store file physically using FileStorageService
        String relativePath = fileStorageService.store(file, "contracts");

        contract.setApplication(application);
        contract.setFileUrl(relativePath);
        contract.setStatus(ContractStatus.SENT);
        contract.setDocumentHash(docHash);
        contract.setRevisionReason(null);
        contract.setRevisionRequestedAt(null);

        contract = contractRepository.save(contract);

        // Update application state machine transition to CONTRACT_SENT if not already there
        if (application.getStatus() != ApplicationStatus.CONTRACT_SENT &&
            application.getStatus() != ApplicationStatus.CONTRACT_SIGNED &&
            application.getStatus() != ApplicationStatus.INTERNING) {
            application.setStatus(ApplicationStatus.CONTRACT_SENT);
            applicationRepository.save(application);
        }

        // Record Audit Event (US-057)
        String auditAction = isReplacement ? "CONTRACT_REPLACED" : "CONTRACT_UPLOADED";
        try {
            auditLogService.log(
                    hrUserId,
                    "hr@holaho.com",
                    auditAction,
                    "Contract",
                    contract.getId(),
                    "SUCCESS",
                    String.format("%s file hợp đồng PDF (Hash: %s) cho đơn #%d", isReplacement ? "Thay thế" : "Tải lên", docHash, applicationId)
            );
        } catch (Exception e) {
            log.warn("Could not record audit log: {}", e.getMessage());
        }

        log.info("HR user {} successfully {} contract {} for application {}", hrUserId, isReplacement ? "replaced" : "uploaded", contract.getId(), applicationId);

        // Notify Intern
        if (application.getIntern() != null && application.getIntern().getUser() != null) {
            try {
                notificationService.createNotification(
                        application.getIntern().getUser().getId(),
                        NotificationType.SYSTEM,
                        isReplacement ? "Hợp đồng thực tập được thay thế" : "Hợp đồng thực tập mới",
                        isReplacement ? "HR đã cập nhật file hợp đồng mới cho đơn ứng tuyển của bạn. Vui lòng xem và xác nhận." : "HR đã tải lên hợp đồng thực tập cho đơn ứng tuyển của bạn. Vui lòng xem và xác nhận.");
            } catch (Exception e) {
                log.warn("Could not dispatch notification: {}", e.getMessage());
            }
        }

        return mapToResponse(contract);
    }

    @Transactional
    public ContractResponse uploadContract(Long applicationId, MultipartFile file) {
        return uploadContract(applicationId, file, null);
    }

    /**
     * US-051: HR confirms contract (Dual confirmation step).
     */
    @Transactional
    public ContractResponse confirmByHr(Long contractId, Long hrUserId) {
        InternshipContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Contract", contractId));

        if (contract.getStatus() == ContractStatus.SIGNED) {
            throw new ConflictException("Hợp đồng này đã hoàn tất xác nhận và đã có hiệu lực (SIGNED)");
        }

        contract.setStatus(ContractStatus.HR_CONFIRMED);
        contract = contractRepository.save(contract);

        try {
            auditLogService.log(
                    hrUserId,
                    "hr@holaho.com",
                    "CONTRACT_HR_CONFIRMED",
                    "Contract",
                    contract.getId(),
                    "SUCCESS",
                    "HR đã xác nhận tính hợp lệ của hợp đồng #" + contractId
            );
        } catch (Exception e) {
            log.warn("Audit log failed: {}", e.getMessage());
        }

        log.info("HR user {} confirmed contract {}", hrUserId, contractId);
        return mapToResponse(contract);
    }

    /**
     * US-010 & US-061: Intern confirms contract with signature evidence (IP + UserAgent + SHA-256 Hash).
     */
    @Transactional
    public ContractResponse confirmByIntern(Long contractId, Long userId, String clientIp, String userAgent) {
        InternshipContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Contract", contractId));

        // Ownership Security Check: Must belong to the current authenticated Intern
        if (contract.getApplication() == null ||
            contract.getApplication().getIntern() == null ||
            contract.getApplication().getIntern().getUser() == null ||
            !contract.getApplication().getIntern().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền xác nhận hợp đồng này");
        }

        if (contract.getStatus() == ContractStatus.SIGNED) {
            throw new ConflictException("Hợp đồng này đã được xác nhận và có hiệu lực từ trước");
        }

        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        contract.setSignedIp(clientIp);
        contract.setSignedUserAgent(userAgent);

        Application app = contract.getApplication();
        if (app != null) {
            try {
                stateMachine.validateTransition(app.getStatus(), ApplicationStatus.CONTRACT_SIGNED);
                app.setStatus(ApplicationStatus.CONTRACT_SIGNED);
                stateMachine.validateTransition(app.getStatus(), ApplicationStatus.INTERNING);
                app.setStatus(ApplicationStatus.INTERNING);
            } catch (Exception e) {
                app.setStatus(ApplicationStatus.INTERNING);
            }
            applicationRepository.save(app);

            if (app.getIntern() != null) {
                app.getIntern().setStatus("INTERNING");
            }
        }

        contract = contractRepository.save(contract);

        // Record Signature Evidence Audit Log (US-057 & US-061)
        try {
            String userEmail = contract.getApplication().getIntern().getUser().getEmail();
            auditLogService.createAuditLog(
                    userId,
                    userEmail,
                    "CONTRACT_SIGNED",
                    "Contract",
                    contract.getId(),
                    "SUCCESS",
                    String.format("Thực tập sinh ký xác nhận hợp đồng #%d (Hash: %s)", contractId, contract.getDocumentHash()),
                    null,
                    null,
                    clientIp,
                    userAgent
            );
        } catch (Exception e) {
            log.warn("Could not record signature audit log: {}", e.getMessage());
        }

        log.info("Intern user {} confirmed contract {} from IP {}", userId, contractId, clientIp);

        // Publish notification
        if (contract.getApplication() != null &&
            contract.getApplication().getIntern() != null &&
            contract.getApplication().getIntern().getUser() != null) {
            try {
                notificationService.createNotification(
                        contract.getApplication().getIntern().getUser().getId(),
                        NotificationType.SYSTEM,
                        "Hợp đồng đã có hiệu lực 🎉",
                        "Chúc mừng! Hợp đồng thực tập của bạn đã được xác nhận. Trạng thái hiện tại: INTERNING.");
            } catch (Exception e) {
                log.warn("Could not dispatch notification: {}", e.getMessage());
            }
        }

        return mapToResponse(contract);
    }

    @Transactional
    public ContractResponse confirmByIntern(Long contractId, Long userId) {
        return confirmByIntern(contractId, userId, "127.0.0.1", "Web Browser");
    }

    @Transactional
    public ContractResponse signContract(Long id, Long userId) {
        return confirmByIntern(id, userId);
    }

    /**
     * US-060: Intern requests contract revision.
     */
    @Transactional
    public ContractResponse requestRevision(Long contractId, Long userId, ContractRevisionRequest request) {
        InternshipContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Contract", contractId));

        if (contract.getApplication() == null ||
            contract.getApplication().getIntern() == null ||
            contract.getApplication().getIntern().getUser() == null ||
            !contract.getApplication().getIntern().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền yêu cầu chỉnh sửa hợp đồng này");
        }

        if (contract.getStatus() == ContractStatus.SIGNED) {
            throw new ConflictException("Hợp đồng đã ký hoàn tất không thể gửi yêu cầu chỉnh sửa");
        }

        contract.setStatus(ContractStatus.NEEDS_REVISION);
        contract.setRevisionReason(request.getReason());
        contract.setRevisionRequestedAt(LocalDateTime.now());

        contract = contractRepository.save(contract);

        // Audit Log Event
        try {
            auditLogService.log(
                    userId,
                    contract.getApplication().getIntern().getUser().getEmail(),
                    "CONTRACT_REVISION_REQUESTED",
                    "Contract",
                    contract.getId(),
                    "SUCCESS",
                    "Thực tập sinh gửi yêu cầu chỉnh sửa hợp đồng với lý do: " + request.getReason()
            );
        } catch (Exception e) {
            log.warn("Audit log error: {}", e.getMessage());
        }

        log.info("Intern user {} requested revision for contract {}", userId, contractId);
        return mapToResponse(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> getMyContracts(Long userId) {
        return contractRepository.findByApplication_Intern_User_Id(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> getAllContracts() {
        return contractRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractResponse getContract(Long id) {
        InternshipContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contract", id));
        return mapToResponse(contract);
    }

    /**
     * US-056: Retrieves file resource for downloading PDF with security check.
     */
    @Transactional(readOnly = true)
    public Resource getContractFileResource(Long contractId, Long currentUserId, boolean isHrOrAdmin) {
        InternshipContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Contract", contractId));

        if (!isHrOrAdmin) {
            if (contract.getApplication() == null ||
                contract.getApplication().getIntern() == null ||
                contract.getApplication().getIntern().getUser() == null ||
                !contract.getApplication().getIntern().getUser().getId().equals(currentUserId)) {
                throw new ForbiddenException("Bạn không có quyền xem hoặc tải file hợp đồng này");
            }
        }

        if (contract.getFileUrl() != null && !contract.getFileUrl().isBlank()) {
            Path filePath = fileStorageService.getPath(contract.getFileUrl());
            if (filePath.toFile().exists()) {
                return new FileSystemResource(filePath.toFile());
            }
        }

        // Fallback to dynamic PDF generation if storage file does not exist
        byte[] pdfBytes = generateContractPdf(contract);
        return new org.springframework.core.io.ByteArrayResource(pdfBytes);
    }

    public byte[] generateContractPdf(InternshipContract contract) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font bodyFont = new Font(Font.HELVETICA, 12, Font.NORMAL);

        document.add(new Paragraph("INTERNSHIP CONTRACT", titleFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Between: HoLaHo IMS", bodyFont));
        String internName = contract.getApplication() != null && contract.getApplication().getIntern() != null && contract.getApplication().getIntern().getUser() != null
                ? contract.getApplication().getIntern().getUser().getFullName()
                : "Intern";
        document.add(new Paragraph("And: " + internName, bodyFont));
        document.add(new Paragraph(" ", bodyFont));
        document.add(new Paragraph("Status: " + contract.getStatus().name(), bodyFont));
        document.add(new Paragraph("Generated at: " + LocalDateTime.now(), bodyFont));

        document.close();
        return out.toByteArray();
    }

    private String calculateSha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "sha256-hash-placeholder";
        }
    }

    private ContractResponse mapToResponse(InternshipContract contract) {
        String internName = "N/A";
        Long internId = null;

        if (contract.getApplication() != null && contract.getApplication().getIntern() != null) {
            internId = contract.getApplication().getIntern().getId();
            if (contract.getApplication().getIntern().getUser() != null) {
                internName = contract.getApplication().getIntern().getUser().getFullName();
            }
        }

        String fileUrl = contract.getFileUrl();
        String fileName = extractFileName(fileUrl);

        return ContractResponse.builder()
                .id(contract.getId())
                .applicationId(contract.getApplication() != null ? contract.getApplication().getId() : null)
                .internId(internId)
                .internName(internName)
                .fileName(fileName)
                .fileUrl(fileUrl != null ? "/api/v1/contracts/" + contract.getId() + "/download" : null)
                .fileSize(1024L)
                .mimeType("application/pdf")
                .status(contract.getStatus() != null ? contract.getStatus().name() : "SENT")
                .uploadedAt(contract.getCreatedAt())
                .hrConfirmedAt(contract.getCreatedAt())
                .internConfirmedAt(contract.getStatus() == ContractStatus.SIGNED ? contract.getSignedAt() : null)
                .signedAt(contract.getSignedAt())
                .createdAt(contract.getCreatedAt())
                .revisionReason(contract.getRevisionReason())
                .revisionRequestedAt(contract.getRevisionRequestedAt())
                .documentHash(contract.getDocumentHash())
                .signedIp(contract.getSignedIp())
                .build();
    }

    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return "contract.pdf";
        int idx = fileUrl.lastIndexOf('/');
        String name = idx >= 0 ? fileUrl.substring(idx + 1) : fileUrl;
        return name.isBlank() ? "contract.pdf" : name;
    }
}
