package com.example.backend.service;

import com.example.backend.dto.request.ApplicationSubmitRequest;
import com.example.backend.dto.request.CreateApplicationRequest;
import com.example.backend.dto.request.ReviewApplicationRequest;
import com.example.backend.dto.response.ApplicationResponse;
import com.example.backend.entity.Application;
import com.example.backend.entity.ApplicationReview;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.User;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.enums.ReviewDecision;
import com.example.backend.exception.*;
import com.example.backend.repository.ApplicationRepository;
import com.example.backend.repository.ApplicationReviewRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationReviewRepository reviewRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public ApplicationResponse submit(ApplicationSubmitRequest request, Long userId) {
        InternProfile intern = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + userId));

        Long internId = intern.getId();

        // Check if already has pending/approved application
        boolean hasPending = applicationRepository.existsByIntern_IdAndStatus(
                internId, ApplicationStatus.SUBMITTED);

        if (hasPending) {
            throw new ConflictException("Bạn đã có đơn ứng tuyển đang chờ xét duyệt");
        }

        Application application = new Application();
        application.setIntern(intern);
        application.setPosition(request.getPosition());
        application.setNote(request.getNote());
        application.setAppliedAt(LocalDateTime.now());
        application.setStatus(ApplicationStatus.SUBMITTED);

        application = applicationRepository.save(application);

        log.info("Application submitted by intern: {}", internId);

        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getByInternId(Long internId) {
        List<Application> applications = applicationRepository.findByIntern_Id(internId);
        return applications.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request, Long internId) {
        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new RuntimeException("Intern profile not found: " + internId));

        // Check if already has pending/approved application
        if (applicationRepository.existsByIntern_IdAndStatus(internId, ApplicationStatus.SUBMITTED) ||
                applicationRepository.existsByIntern_IdAndStatus(internId, ApplicationStatus.APPROVED)) {
            throw new RuntimeException("You already have a pending or approved application");
        }

        Application application = new Application();
        application.setIntern(intern);
        application.setPosition(request.getPosition());
        application.setAppliedAt(LocalDateTime.now());
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setNote(request.getNote());

        application = applicationRepository.save(application);
        log.info("Created application for intern: {}", internId);

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application", id));
        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(Long userId) {
        InternProfile intern = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Intern profile not found"));

        List<Application> applications = applicationRepository.findByIntern_Id(intern.getId());
        return applications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> searchApplications(ApplicationStatus status, String keyword, Pageable pageable) {
        return applicationRepository.searchApplications(status, keyword, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return mapToResponse(application);
    }

    @Transactional
    public ApplicationResponse reviewApplication(Long applicationId, ReviewApplicationRequest request,
            Long reviewerId) {
        Application application = applicationRepository.findByIdWithIntern(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        if (application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new RuntimeException(
                    "Only SUBMITTED applications can be reviewed. Current status: " + application.getStatus());
        }

        // Check if already reviewed
        if (reviewRepository.existsByApplicationId(applicationId)) {
            throw new RuntimeException("Application already reviewed");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found: " + reviewerId));

        // Create review
        ApplicationReview review = new ApplicationReview();
        review.setApplication(application);
        review.setReviewer(reviewer);
        review.setDecision(request.decision());
        review.setComment(request.comment());
        review.setDecidedAt(LocalDateTime.now());

        reviewRepository.save(review);

        // Update application status
        if (request.decision() == ReviewDecision.APPROVE) {
            application.setStatus(ApplicationStatus.APPROVED);
        } else {
            application.setStatus(ApplicationStatus.REJECTED);
        }

        application = applicationRepository.save(application);
        log.info("Reviewed application {} with decision: {}", applicationId, request.decision());

        // Send notification email
        try {
            String toEmail = application.getIntern().getUser().getEmail();
            String internName = application.getIntern().getUser().getFullName();
            String decisionStr = (request.decision() == ReviewDecision.APPROVE) ? "ĐƯỢC CHẤP NHẬN" : "BỊ TỪ CHỐI";

            emailService.sendApplicationResultEmail(toEmail, internName, decisionStr, request.comment());
        } catch (Exception e) {
            log.warn("Failed to send notification email", e);
        }

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        Application application = applicationRepository.findByIdWithIntern(id)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));
        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByInternId(Long internId) {
        return applicationRepository.findByInternId(internId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse.ApplicationReviewResponse> getReviewsByApplicationId(Long applicationId) {
        return reviewRepository.findByApplicationIdOrderByDecidedAtDesc(applicationId).stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());
    }

    private ApplicationResponse mapToResponse(Application application) {
        List<ApplicationResponse.ApplicationReviewResponse> reviews = reviewRepository
                .findByApplicationIdOrderByDecidedAtDesc(application.getId()).stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());

        return ApplicationResponse.builder()
                .id(application.getId())
                .internId(application.getIntern().getId())
                .internName(application.getIntern().getUser().getFullName())
                .internEmail(application.getIntern().getUser().getEmail())
                .position(application.getPosition())
                .appliedAt(application.getAppliedAt())
                .status(String.valueOf(application.getStatus()))
                .note(application.getNote())
                .reviews(reviews)
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    private ApplicationResponse toResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setInternId(application.getIntern().getId());
        response.setInternName(application.getIntern().getUser().getFullName());
        response.setInternEmail(application.getIntern().getUser().getEmail());
        response.setPosition(application.getPosition());
        response.setAppliedAt(application.getAppliedAt());
        response.setStatus(application.getStatus().name());
        response.setNote(application.getNote());
        response.setCreatedAt(application.getCreatedAt());
        return response;
    }

    private ApplicationResponse.ApplicationReviewResponse mapReviewToResponse(ApplicationReview review) {
        return ApplicationResponse.ApplicationReviewResponse.builder()
                .id(review.getId())
                .applicationId(review.getApplication().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .decision(review.getDecision())
                .comment(review.getComment())
                .decidedAt(review.getDecidedAt())
                .build();
    }
}
