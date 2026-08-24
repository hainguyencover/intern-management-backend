package com.holaho.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.entity.ApplicationReview;
import com.holaho.intern.entity.Program;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.repository.ApplicationReviewRepository;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.ApplicationStatusHistoryRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.dto.request.ApplicationSubmitRequest;
import com.holaho.intern.shared.dto.request.ReviewApplicationRequest;
import com.holaho.intern.shared.dto.response.ApplicationResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.enums.ReviewDecision;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.workflow.ApplicationStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    private InternDocumentRepository internDocumentRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ApplicationStatusHistoryRepository statusHistoryRepository;
    @Mock
    private ApplicationStateMachine stateMachine;

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
        internUser.setEmailVerified(true);

        intern = new InternProfile();
        intern.setId(1L);
        intern.setUser(internUser);
        intern.setCvUrl("https://storage.holaho.com/cv.pdf");

        program = new Program();
        program.setId(1L);
        program.setStatus(ProgramStatus.ACTIVE);
    }

    @Test
    void submit_Success() {
        // Arrange
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.setProgramId(1L);
        request.setPosition("Java Developer");

        when(userRepository.findById(1L)).thenReturn(Optional.of(internUser));
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

        when(userRepository.findById(1L)).thenReturn(Optional.of(internUser));
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

        Role internRole = new Role();
        internRole.setCode("INTERN");

        when(applicationRepository.findByIdWithIntern(applicationId)).thenReturn(Optional.of(application));
        when(reviewRepository.existsByApplicationId(applicationId)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewer));
        when(roleRepository.findByCode("INTERN")).thenReturn(Optional.of(internRole));
        when(applicationRepository.save(any())).thenReturn(application);

        // Act
        ApplicationResponse response = applicationService.reviewApplication(applicationId, request, 2L);

        // Assert
        assertNotNull(response);
        assertEquals(ApplicationStatus.APPROVED.name(), response.getStatus());
        verify(reviewRepository).save(any(ApplicationReview.class));
        verify(eventPublisher).publishEvent(any(com.holaho.intern.shared.events.DomainEvents.ApplicationAcceptedEvent.class));
    }

    @Test
    void reviewApplication_Reject_Success() {
        // Arrange
        Long applicationId = 100L;
        ReviewApplicationRequest request = new ReviewApplicationRequest(ReviewDecision.REJECT, "GPA score insufficient");

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
        assertEquals(ApplicationStatus.REJECTED.name(), response.getStatus());
        verify(reviewRepository).save(any(ApplicationReview.class));
        verify(eventPublisher).publishEvent(any(com.holaho.intern.shared.events.DomainEvents.ApplicationRejectedEvent.class));
    }

    @Test
    void reviewApplication_Reject_MissingComment_ThrowsBadRequest() {
        Long applicationId = 100L;
        ReviewApplicationRequest request = new ReviewApplicationRequest(ReviewDecision.REJECT, "  ");

        Application application = new Application();
        application.setId(applicationId);
        application.setIntern(intern);
        application.setStatus(ApplicationStatus.SUBMITTED);

        when(applicationRepository.findByIdWithIntern(applicationId)).thenReturn(Optional.of(application));

        assertThrows(BadRequestException.class, () -> applicationService.reviewApplication(applicationId, request, 2L));
    }

    @Test
    void reviewApplication_Approve_MissingCv_ThrowsConflict() {
        Long applicationId = 100L;
        ReviewApplicationRequest request = new ReviewApplicationRequest(ReviewDecision.APPROVE, "Good profile");

        intern.setCvUrl(null);
        Application application = new Application();
        application.setId(applicationId);
        application.setIntern(intern);
        application.setStatus(ApplicationStatus.SUBMITTED);

        when(applicationRepository.findByIdWithIntern(applicationId)).thenReturn(Optional.of(application));

        assertThrows(ConflictException.class, () -> applicationService.reviewApplication(applicationId, request, 2L));
    }

    @Test
    void reviewApplication_Fail_AlreadyReviewed() {
        // Arrange
        Long applicationId = 100L;
        ReviewApplicationRequest request = new ReviewApplicationRequest(ReviewDecision.APPROVE, "Good profile");

        Application application = new Application();
        application.setId(applicationId);
        application.setIntern(intern);
        application.setStatus(ApplicationStatus.SUBMITTED);

        when(applicationRepository.findByIdWithIntern(applicationId)).thenReturn(Optional.of(application));
        when(reviewRepository.existsByApplicationId(applicationId)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> applicationService.reviewApplication(applicationId, request, 2L));
    }
}
