package com.holaho.intern.service;

import com.holaho.intern.entity.Program;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;

import com.holaho.intern.shared.dto.request.ApplicationSubmitRequest;
import com.holaho.intern.shared.dto.request.CreateApplicationRequest;
import com.holaho.intern.shared.dto.request.ReviewApplicationRequest;
import com.holaho.intern.shared.dto.response.ApplicationResponse;
import com.holaho.intern.entity.Application;
import com.holaho.intern.entity.ApplicationReview;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.ReviewDecision;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.repository.ApplicationReviewRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.user.repository.RoleRepository;
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
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final AiService aiService;
    private final StorageService storageService;

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

        com.holaho.intern.entity.Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new NotFoundException("Program not found: " + request.getProgramId()));

        Application application = new Application();
        application.setIntern(intern);
        application.setProgram(program);
        application.setPosition(request.getPosition());
        application.setNote(request.getNote());
        application.setAppliedAt(LocalDateTime.now());
        application.setStatus(ApplicationStatus.SUBMITTED);

        application = applicationRepository.save(application);

        log.info("Application submitted by intern: {} for program: {}", internId, request.getProgramId());

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
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        // Check if already has pending/approved application
        if (applicationRepository.existsByIntern_IdAndStatus(internId, ApplicationStatus.SUBMITTED) ||
                applicationRepository.existsByIntern_IdAndStatus(internId, ApplicationStatus.APPROVED)) {
            throw new ConflictException("Bạn đã có đơn ứng tuyển đang chờ hoặc đã được duyệt");
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
                .orElseThrow(() -> new NotFoundException("Intern profile not found"));

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
                .orElseThrow(() -> new NotFoundException("Application not found"));
        return mapToResponse(application);
    }

    @Transactional
    public ApplicationResponse reviewApplication(Long applicationId, ReviewApplicationRequest request,
            Long reviewerId) {
        Application application = applicationRepository.findByIdWithIntern(applicationId)
                .orElseThrow(() -> new NotFoundException("Application", applicationId));

        if (application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new BadRequestException(
                    "Chỉ có thể duyệt đơn ứng tuyển ở trạng thái SUBMITTED. Trạng thái hiện tại: "
                            + application.getStatus());
        }

        // Check if already reviewed
        if (reviewRepository.existsByApplicationId(applicationId)) {
            throw new ConflictException("Đơn ứng tuyển đã được duyệt trước đó");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Reviewer", reviewerId));

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

            // Auto-transition candidate profile status to ONBOARDING
            InternProfile profile = application.getIntern();
            profile.setStatus("ONBOARDING");
            internProfileRepository.save(profile);

            // Assign ROLE_INTERN to user
            User user = profile.getUser();
            com.holaho.intern.user.entity.Role internRole = roleRepository.findByCode("INTERN")
                    .orElseThrow(() -> new NotFoundException("Role INTERN không tồn tại"));
            user.getRoles().add(internRole);
            userRepository.save(user);

            log.info("Auto activated InternProfile to ONBOARDING and assigned ROLE_INTERN for user: {}", user.getEmail());
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

    @Transactional
    public void triggerAiScreening(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        String cvUrl = application.getIntern().getCvUrl();
        if (cvUrl == null || cvUrl.isEmpty()) {
            log.warn("Cannot trigger AI screening for application {}: No CV URL found", applicationId);
            return;
        }

        try {
            log.info("Triggering AI screening for application {} with CV: {}", applicationId, cvUrl);
            org.springframework.core.io.Resource resource = storageService.loadFileAsResource(cvUrl);
            byte[] fileContent = resource.getContentAsByteArray();

            var screeningResult = aiService.screenCv(fileContent, cvUrl);

            application.setAiScore(screeningResult.getScore());
            application.setAiSkills(String.join(", ", screeningResult.getSkills()));
            application.setAiSummary(screeningResult.getSummary());
            application.setAiRecommendation(screeningResult.getRecommendation());

            applicationRepository.save(application);
            log.info("AI screening completed for application {}", applicationId);
        } catch (Exception e) {
            log.error("Failed to perform AI screening for application {}", applicationId, e);
        }
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        Application application = applicationRepository.findByIdWithIntern(id)
                .orElseThrow(() -> new NotFoundException("Application", id));
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
                .aiScore(application.getAiScore())
                .aiSkills(
                        application.getAiSkills() != null ? List.of(application.getAiSkills().split(", ")) : List.of())
                .aiSummary(application.getAiSummary())
                .aiRecommendation(application.getAiRecommendation())
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
