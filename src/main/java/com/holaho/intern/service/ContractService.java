package com.holaho.intern.service;

import com.holaho.intern.notification.service.NotificationService;


import com.holaho.intern.shared.dto.response.ContractResponse;
import com.holaho.intern.entity.Application;
import com.holaho.intern.intern.entity.InternshipContract;
import com.holaho.intern.shared.enums.ContractStatus;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.intern.repository.InternshipContractRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
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

    @Transactional
    public ContractResponse uploadContract(Long applicationId, MultipartFile file) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application", applicationId));

        InternshipContract contract = contractRepository.findByApplication_Id(applicationId)
                .orElse(new InternshipContract());

        contract.setApplication(application);
        // In a real app, we would upload to Cloudinary or S3 and store URL.
        // For now, we'll simulate a URL.
        contract.setFileUrl(
                "https://storage.example.com/contracts/" + application.getIntern().getStudentCode() + ".pdf");
        contract.setStatus(ContractStatus.PENDING_SIGN);

        contract = contractRepository.save(contract);
        log.info("Uploaded contract for application: {}", applicationId);

        return mapToResponse(contract);
    }

    @Transactional
    public ContractResponse signContract(Long id, Long userId) {
        InternshipContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contract", id));

        if (!contract.getApplication().getIntern().getUser().getId().equals(userId)) {
            throw new RuntimeException("BÃƒÂ¡Ã‚ÂºÃ‚Â¡n khÃƒÆ’Ã‚Â´ng cÃƒÆ’Ã‚Â³ quyÃƒÂ¡Ã‚Â»Ã‚Ân kÃƒÆ’Ã‚Â½ hÃƒÂ¡Ã‚Â»Ã‚Â£p Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ng nÃƒÆ’Ã‚Â y");
        }

        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        contract = contractRepository.save(contract);

        log.info("Contract {} has been signed digitally by user {}", id, userId);

        notificationService.createNotification(
                1L, // Fallback to admin/hr user ID 1 for system notifications
                NotificationType.SYSTEM,
                "HÃƒÂ¡Ã‚Â»Ã‚Â£p Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ng Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â£ Ãƒâ€žÃ¢â‚¬ËœÃƒâ€ Ã‚Â°ÃƒÂ¡Ã‚Â»Ã‚Â£c kÃƒÆ’Ã‚Â½",
                "ThÃƒÂ¡Ã‚Â»Ã‚Â±c tÃƒÂ¡Ã‚ÂºÃ‚Â­p sinh " + contract.getApplication().getIntern().getUser().getFullName() + " Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â£ kÃƒÆ’Ã‚Â½ hÃƒÂ¡Ã‚Â»Ã‚Â£p Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ng.");
        return mapToResponse(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> getMyContracts(Long userId) {
        return contractRepository.findByApplication_Intern_User_Id(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractResponse getContract(Long id) {
        InternshipContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contract", id));
        return mapToResponse(contract);
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
        document.add(new Paragraph("Between: Antigravity Co., Ltd", bodyFont));
        document.add(new Paragraph("And: " + contract.getApplication().getIntern().getUser().getFullName(), bodyFont));
        document.add(
                new Paragraph("Student Code: " + contract.getApplication().getIntern().getStudentCode(), bodyFont));
        document.add(new Paragraph(" ", bodyFont));
        document.add(
                new Paragraph("Subject: Internship for " + contract.getApplication().getProgram().getName(), bodyFont));
        document.add(new Paragraph(" ", bodyFont));
        document.add(new Paragraph(
                "By signing this document, the intern agrees to the company's rules and regulations.", bodyFont));
        document.add(new Paragraph(" ", bodyFont));
        document.add(new Paragraph("Generated at: " + LocalDateTime.now(), bodyFont));

        document.close();
        return out.toByteArray();
    }

    private ContractResponse mapToResponse(InternshipContract contract) {
        return ContractResponse.builder()
                .id(contract.getId())
                .applicationId(contract.getApplication().getId())
                .internId(contract.getApplication().getIntern().getId())
                .internName(contract.getApplication().getIntern().getUser().getFullName())
                .fileUrl(contract.getFileUrl())
                .status(contract.getStatus().name())
                .signedAt(contract.getSignedAt())
                .createdAt(contract.getCreatedAt())
                .build();
    }
}

