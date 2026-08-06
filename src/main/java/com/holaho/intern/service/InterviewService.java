package com.holaho.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.entity.Interview;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.dto.request.InterviewScheduleRequest;
import com.holaho.intern.shared.dto.response.InterviewResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.repository.InterviewRepository;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public InterviewResponse scheduleInterview(InterviewScheduleRequest request) {
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new NotFoundException("Application not found: " + request.getApplicationId()));

        User interviewer = userRepository.findById(request.getInterviewerId())
                .orElseThrow(() -> new NotFoundException("Interviewer not found: " + request.getInterviewerId()));

        Interview interview = Interview.builder()
                .application(application)
                .scheduledTime(request.getScheduledTime())
                .location(request.getLocation())
                .interviewer(interviewer)
                .status("SCHEDULED")
                .build();

        // Update application status to INTERVIEWING
        application.setStatus(ApplicationStatus.INTERVIEWING);
        applicationRepository.save(application);

        interview = interviewRepository.save(interview);
        log.info("Scheduled interview for application: {} with interviewer: {}", application.getId(), interviewer.getEmail());

        return mapToResponse(interview);
    }

    @Transactional
    public InterviewResponse submitFeedback(Long interviewId, String feedback, String status) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new NotFoundException("Interview not found: " + interviewId));

        interview.setFeedback(feedback);
        interview.setStatus(status); // COMPLETED, CANCELLED

        if ("COMPLETED".equalsIgnoreCase(status)) {
            // Can transition to APPROVED or keep it as completed pending overall decision
            log.info("Interview feedback submitted for interview: {}", interviewId);
        }

        interview = interviewRepository.save(interview);
        return mapToResponse(interview);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByApplication(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private InterviewResponse mapToResponse(Interview interview) {
        return InterviewResponse.builder()
                .id(interview.getId())
                .applicationId(interview.getApplication().getId())
                .scheduledTime(interview.getScheduledTime())
                .location(interview.getLocation())
                .interviewerId(interview.getInterviewer().getId())
                .interviewerName(interview.getInterviewer().getFullName())
                .status(interview.getStatus())
                .feedback(interview.getFeedback())
                .build();
    }
}
