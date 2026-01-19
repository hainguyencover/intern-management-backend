package com.example.backend.service;

import com.example.backend.dto.response.ContractResponse;
import com.example.backend.entity.*;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.enums.ContractStatus;
import com.example.backend.exception.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private final InternshipContractRepository contractRepository;
    private final ApplicationRepository applicationRepository;
    private final FileStorageService fileStorageService;
    private final InternProfileRepository internProfileRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Transactional
    public ContractResponse uploadContract(Long applicationId, MultipartFile file) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application", applicationId));

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new BadRequestException("Chỉ có thể tạo hợp đồng cho hồ sơ đã được duyệt");
        }

        // Check if contract already exists
        Optional<InternshipContract> existing = contractRepository.findByApplication_Id(applicationId);
        if (existing.isPresent()) {
            throw new ConflictException("Hồ sơ này đã có hợp đồng");
        }

        // Save file
        String fileUrl = fileStorageService.store(file, "contracts");

        // Create contract
        InternshipContract contract = new InternshipContract();
        contract.setApplication(application);
        contract.setFileUrl(fileUrl);
        contract.setStatus(ContractStatus.SENT);

        contract = contractRepository.save(contract);

        // Update application status
        application.setStatus(ApplicationStatus.CONTRACT_SENT);
        applicationRepository.save(application);

        log.info("Contract uploaded for application {}", applicationId);

        // Notify intern
        notificationService.notifyInternAboutContract(contract);
        emailService.sendContractEmail(contract);

        return toResponse(contract);
    }

    @Transactional
    public ContractResponse signContract(Long id, Long userId) {
        InternshipContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contract", id));

        // Verify ownership
        Long internUserId = contract.getApplication().getIntern().getUser().getId();
        if (!internUserId.equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền ký hợp đồng này");
        }

        if (contract.getStatus() != ContractStatus.SENT) {
            throw new BadRequestException("Hợp đồng đã được ký hoặc đã hủy");
        }

        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        contract = contractRepository.save(contract);

        // Update application status
        Application application = contract.getApplication();
        application.setStatus(ApplicationStatus.CONTRACT_SIGNED);
        applicationRepository.save(application);

        log.info("Contract {} signed by user {}", id, userId);

        return toResponse(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> getMyContracts(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + userId));

        List<InternshipContract> contracts = contractRepository.findByInternId(profile.getId());
        return contracts.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> getAllContracts() {
        return contractRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractResponse getContract(Long id) {
        InternshipContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contract", id));
        return toResponse(contract);
    }

    private ContractResponse toResponse(InternshipContract contract) {
        Application app = contract.getApplication();
        InternProfile intern = app.getIntern();

        return ContractResponse.builder()
                .id(contract.getId())
                .applicationId(app.getId())
                .internId(intern.getId())
                .internName(intern.getUser().getFullName())
                .fileUrl("/api/files/" + contract.getFileUrl())
                .status(contract.getStatus().name())
                .signedAt(contract.getSignedAt())
                .createdAt(contract.getCreatedAt())
                .build();
    }
}
