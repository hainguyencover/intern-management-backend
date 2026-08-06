package com.holaho.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.entity.ApplicationReview;
import com.holaho.intern.entity.Program;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.repository.ApplicationReviewRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.dto.request.ApplicationSubmitRequest;
import com.holaho.intern.shared.dto.request.ReviewApplicationRequest;
import com.holaho.intern.shared.dto.response.ApplicationResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.ReviewDecision;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationReviewRepository reviewRepository;
    @Mock
    private InternProfileRepository internProfileRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ApplicationService applicationService;

    private InternProfile intern;
    private User internUser;
    private Program program;

    @BeforeEach
    void setUp() {
        internUser = new User();
        internUser.setId(1L);
        internUser.setEmail("intern@test.com");
        internUser.setFullName("Intern Test");

        intern = new InternProfile();
        intern.setId(1L);
        intern.setUser(internUser);

        program = new Program();
        program.setId(1L);
    }

    @Test
    void submit_Success() {
        // Arrange
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.setProgramId(1L);
        request.setPosition("Java Developer");

        when(internProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(intern));
        when(applicationRepository.existsByIntern_IdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(false);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        // Act
        ApplicationResponse response = applicationService.submit(request, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(ApplicationStatus.SUBMITTED.name(), response.getStatus());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void submit_Fail_AlreadyHasPending() {
        // Arrange
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.setProgramId(1L);

        when(internProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(intern));
        when(applicationRepository.existsByIntern_IdAndStatus(1L, ApplicationStatus.SUBMITTED)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> applicationService.submit(request, 1L));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void reviewApplication_Approve_Success() {
        // Arrange
        Long applicationId = 100L;
        ReviewApplicationRequest request = new ReviewApplicationRequest(ReviewDecision.APPROVE, "Good profile");

        Application application = new Application();
        application.setId(applicationId);
        application.setIntern(intern);
        application.setStatus(ApplicationStatus.SUBMITTED);

        User reviewer = new User();
        reviewer.setId(2L);

        when(applicationRepository.findByIdWithIntern(applicationId)).thenReturn(Optional.of(application));
        when(reviewRepository.existsByApplicationId(applicationId)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewer));

        when(applicationRepository.save(any())).thenReturn(application);

        // Act
        ApplicationResponse response = applicationService.reviewApplication(applicationId, request, 2L);

        // Assert
        assertNotNull(response);
        assertEquals(ApplicationStatus.APPROVED.name(), response.getStatus());
        verify(reviewRepository).save(any(ApplicationReview.class));
        verify(emailService).sendApplicationResultEmail(eq("intern@test.com"), anyString(), contains("CHẤP NHẬN"),
                eq("Good profile"));
    }

    @Test
    void reviewApplication_Fail_AlreadyReviewed() {
        // Arrange
        Long applicationId = 100L;
        ReviewApplicationRequest request = new ReviewApplicationRequest(ReviewDecision.APPROVE, "Good profile");

        Application application = new Application();
        application.setId(applicationId);
        application.setStatus(ApplicationStatus.SUBMITTED);

        when(applicationRepository.findByIdWithIntern(applicationId)).thenReturn(Optional.of(application));
        when(reviewRepository.existsByApplicationId(applicationId)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> applicationService.reviewApplication(applicationId, request, 2L));
    }
}
