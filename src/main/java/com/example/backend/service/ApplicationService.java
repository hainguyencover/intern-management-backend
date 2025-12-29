package com.example.backend.service;

import com.example.backend.dto.request.ApplicationRequest;
import com.example.backend.dto.response.ApplicationResponse;
import com.example.backend.entity.Application;
import com.example.backend.entity.InternProfile;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.ApplicationRepository;
import com.example.backend.repository.InternProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final InternProfileRepository internProfileRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
            InternProfileRepository internProfileRepository) {
        this.applicationRepository = applicationRepository;
        this.internProfileRepository = internProfileRepository;
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
