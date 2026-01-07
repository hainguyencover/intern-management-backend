package com.example.backend.service;

import com.example.backend.dto.ApplicationDetailDto;
import com.example.backend.dto.ApplicationReviewDto;
import com.example.backend.dto.ApplicationSummaryDto;
import com.example.backend.dto.request.ApplicationRequest;
import com.example.backend.dto.request.ReviewApplicationRequest;
import com.example.backend.dto.response.ApplicationResponse;
import com.example.backend.entity.Application;
import com.example.backend.entity.ApplicationReview;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.User;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.enums.ReviewDecision;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.ApplicationRepository;
import com.example.backend.repository.ApplicationReviewRepository;
import com.example.backend.repository.InternProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationReviewRepository reviewRepository;
    private final InternProfileRepository internProfileRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public Page<ApplicationSummaryDto> listForHr(ApplicationStatus status, Pageable pageable) {
        Page<Application> page = (status == null)
                ? applicationRepository.findAll(pageable)
                : applicationRepository.findAllByStatus(status, pageable);

        return page.map(app -> new ApplicationSummaryDto(
                app.getId(),
                app.getIntern().getId(),
                app.getIntern().getUser().getFullName(),
                app.getIntern().getUser().getEmail(),
                app.getPosition(),
                app.getAppliedAt(),
                app.getStatus()
        ));
    }

    @Transactional(readOnly = true)
    public ApplicationDetailDto getDetail(Long id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + id));

        // đảm bảo intern/user được load
        app.getIntern().getUser().getEmail();

        List<ApplicationReviewDto> reviews = reviewRepository
                .findByApplicationIdOrderByDecidedAtDesc(id)
                .stream()
                .map(r -> new ApplicationReviewDto(
                        r.getId(),
                        r.getReviewer().getId(),
                        r.getReviewer().getFullName(),
                        r.getDecision(),
                        r.getComment(),
                        r.getDecidedAt()
                ))
                .toList();

        return new ApplicationDetailDto(
                app.getId(),
                app.getIntern().getId(),
                app.getIntern().getUser().getFullName(),
                app.getIntern().getUser().getEmail(),
                app.getPosition(),
                app.getNote(),
                app.getAppliedAt(),
                app.getStatus(),
                reviews
        );
    }


    @Transactional
    public ApplicationDetailDto review(Long applicationId, ReviewApplicationRequest req) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + applicationId));

        if (app.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED applications can be reviewed. Current=" + app.getStatus());
        }

        User reviewer = currentUserService.getCurrentUserEntity();

        ApplicationReview review = new ApplicationReview();
        review.setApplication(app);
        review.setReviewer(reviewer);
        review.setDecision(req.decision());
        review.setComment(req.comment());
        review.setDecidedAt(LocalDateTime.now());

        reviewRepository.save(review);

        if (req.decision() == ReviewDecision.APPROVE) {
            app.setStatus(ApplicationStatus.APPROVED);
        } else {
            app.setStatus(ApplicationStatus.REJECTED);
        }

        applicationRepository.save(app);

        return getDetail(app.getId());
    }


    @Transactional
    public ApplicationResponse submitApplication(ApplicationRequest req) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        var opt = internProfileRepository.findByUser_Email(principal);
        InternProfile ip = opt.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Intern profile not found for user: " + principal));

        Application app = new Application();
        app.setIntern(ip);
        app.setPosition(req.getPosition());
        app.setNote(req.getNote());
        app.setAppliedAt(LocalDateTime.now());
        app.setStatus(ApplicationStatus.SUBMITTED);

        Application saved = applicationRepository.save(app);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        var opt = internProfileRepository.findByUser_Email(principal);
        InternProfile ip = opt.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Intern profile not found for user: " + principal));

        List<Application> apps = applicationRepository.findByIntern_Id(ip.getId());
        return apps.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ApplicationResponse toResponse(Application a) {
        ApplicationResponse r = new ApplicationResponse();
        r.setId(a.getId());
        r.setInternId(a.getIntern() != null ? a.getIntern().getId() : null);
        r.setPosition(a.getPosition());
        r.setAppliedAt(a.getAppliedAt());
        r.setStatus(a.getStatus());
        r.setNote(a.getNote());
        return r;
    }
}
