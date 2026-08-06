package com.holaho.intern.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.intern.entity.InternDocument;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.service.StorageService;
import com.holaho.intern.shared.dto.StoredFile;
import com.holaho.intern.shared.dto.response.InternDocumentResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.DocumentType;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternDocumentServiceTest {

    @Mock
    private InternDocumentRepository repo;
    @Mock
    private InternProfileRepository internRepo;
    @Mock
    private StorageService storage;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationRepository appRepo;

    @InjectMocks
    private InternDocumentServiceImpl service;

    private InternProfile intern;
    private User internUser;

    @BeforeEach
    void setUp() {
        internUser = new User();
        internUser.setId(1L);
        internUser.setFullName("Intern Test");

        intern = new InternProfile();
        intern.setId(1L);
        intern.setUser(internUser);
    }

    @Test
    void uploadForIntern_SelfUpload_Success() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        StoredFile stored = new StoredFile("test.pdf", "/uploads/test.pdf", 7L);

        when(internRepo.findById(1L)).thenReturn(Optional.of(intern));
        when(storage.saveInternDocument(eq(1L), eq(DocumentType.CV), any())).thenReturn(stored);
        when(repo.findTopByIntern_IdAndTypeOrderByUploadedAtDesc(eq(1L), eq("CV"))).thenReturn(Optional.empty());
        when(repo.save(any(InternDocument.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        InternDocumentResponse response = service.uploadForIntern(1L, DocumentType.CV, file, 1L);

        // Assert
        assertNotNull(response);
        assertEquals("PENDING", response.status());
        assertEquals("/uploads/test.pdf", response.fileUrl());
        verify(repo).save(any(InternDocument.class));
    }

    @Test
    void uploadForIntern_AdminUpload_Success_ApproveAndNotify() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", "content".getBytes());
        StoredFile stored = new StoredFile("contract.pdf", "/uploads/contract.pdf", 100L);

        User admin = new User();
        admin.setId(2L);
        admin.setFullName("Admin HR");

        Application application = new Application();
        application.setStatus(ApplicationStatus.APPROVED);

        when(internRepo.findById(1L)).thenReturn(Optional.of(intern));
        when(storage.saveInternDocument(anyLong(), any(), any())).thenReturn(stored);
        when(repo.findTopByIntern_IdAndTypeOrderByUploadedAtDesc(anyLong(), anyString())).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(appRepo.findByIntern_Id(anyLong()))
                .thenReturn(List.of(application));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act
        InternDocumentResponse response = service.uploadForIntern(1L, DocumentType.INTERNSHIP_CONTRACT, file, 2L);

        // Assert
        assertNotNull(response);
        assertEquals("APPROVED", response.status());
        assertEquals(ApplicationStatus.CONTRACT_SENT, application.getStatus());
        verify(appRepo).save(application);
    }

    @Test
    void getMyDocuments_Success() {
        // Arrange
        when(repo.findByIntern_IdOrderByUploadedAtDesc(1L)).thenReturn(Collections.emptyList());

        // Act
        var result = service.getMyDocuments(1L);

        // Assert
        assertNotNull(result);
        verify(repo).findByIntern_IdOrderByUploadedAtDesc(1L);
    }
}
