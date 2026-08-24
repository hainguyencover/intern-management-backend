package com.holaho.intern.intern.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.intern.entity.InternDocument;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.service.DocumentValidationService;
import com.holaho.intern.service.StorageService;
import com.holaho.intern.shared.dto.StoredFile;
import com.holaho.intern.shared.dto.response.InternDocumentResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.DocumentType;
import com.holaho.intern.shared.exception.ApiException;
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
    @Mock
    private DocumentValidationService validationService;

    @InjectMocks
    private InternDocumentServiceImpl service;

    private InternProfile intern;
    private User internUser;
    private byte[] pdfHeader;

    @BeforeEach
    void setUp() {
        internUser = new User();
        internUser.setId(1L);
        internUser.setFullName("Intern Test");

        intern = new InternProfile();
        intern.setId(1L);
        intern.setTenantId(1L);
        intern.setUser(internUser);

        pdfHeader = new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34 }; // %PDF-1.4
    }

    @Test
    void uploadForIntern_SelfUpload_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", pdfHeader);
        StoredFile stored = new StoredFile("/uploads/cv.pdf", "cv.pdf", (long) pdfHeader.length);

        when(internRepo.findById(1L)).thenReturn(Optional.of(intern));
        when(storage.saveInternDocument(eq(1L), eq(DocumentType.CV), any())).thenReturn(stored);
        when(repo.findTopByIntern_IdAndTypeOrderByUploadedAtDesc(eq(1L), eq("CV"))).thenReturn(Optional.empty());
        when(repo.save(any(InternDocument.class))).thenAnswer(i -> i.getArguments()[0]);

        InternDocumentResponse response = service.uploadForIntern(1L, DocumentType.CV, file, 1L);

        assertNotNull(response);
        assertEquals("PENDING", response.status());
        assertEquals("/uploads/cv.pdf", response.fileUrl());
        verify(validationService).validateDocumentFile(file);
        verify(repo).save(any(InternDocument.class));
    }

    @Test
    void uploadForIntern_AdminUpload_Success_ApproveAndNotify() {
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", pdfHeader);
        StoredFile stored = new StoredFile("/uploads/contract.pdf", "contract.pdf", 100L);

        User admin = new User();
        admin.setId(2L);
        admin.setFullName("Admin HR");

        Application application = new Application();
        application.setStatus(ApplicationStatus.APPROVED);

        when(internRepo.findById(1L)).thenReturn(Optional.of(intern));
        when(storage.saveInternDocument(anyLong(), any(), any())).thenReturn(stored);
        when(repo.findTopByIntern_IdAndTypeOrderByUploadedAtDesc(anyLong(), anyString())).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(appRepo.findByIntern_Id(anyLong())).thenReturn(List.of(application));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        InternDocumentResponse response = service.uploadForIntern(1L, DocumentType.INTERNSHIP_CONTRACT, file, 2L);

        assertNotNull(response);
        assertEquals("APPROVED", response.status());
        assertEquals(ApplicationStatus.CONTRACT_SENT, application.getStatus());
        verify(appRepo).save(application);
    }

    @Test
    void approveDocument_Success() {
        InternDocument doc = new InternDocument();
        doc.setIntern(intern);
        doc.setType("CV");
        doc.setStatus("PENDING");

        User hr = new User();
        hr.setId(2L);
        hr.setFullName("HR Reviewer");

        when(repo.findById(10L)).thenReturn(Optional.of(doc));
        when(userRepository.findById(2L)).thenReturn(Optional.of(hr));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        InternDocumentResponse resp = service.approve(10L, 2L);

        assertNotNull(resp);
        assertEquals("APPROVED", resp.status());
        verify(notificationService).createNotification(eq(1L), any(), anyString(), anyString());
    }

    @Test
    void rejectDocument_WithoutReason_ThrowsException() {
        assertThrows(ApiException.class, () -> service.reject(10L, 2L, "  "));
    }

    @Test
    void rejectDocument_Success() {
        InternDocument doc = new InternDocument();
        doc.setIntern(intern);
        doc.setType("CV");
        doc.setStatus("PENDING");

        User hr = new User();
        hr.setId(2L);

        when(repo.findById(10L)).thenReturn(Optional.of(doc));
        when(userRepository.findById(2L)).thenReturn(Optional.of(hr));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        InternDocumentResponse resp = service.reject(10L, 2L, "File bị lỗi font");

        assertNotNull(resp);
        assertEquals("REJECTED", resp.status());
        assertEquals("File bị lỗi font", resp.rejectionReason());
        verify(notificationService).createNotification(eq(1L), any(), anyString(), contains("File bị lỗi font"));
    }

    @Test
    void hasAllRequiredDocuments_ChecksBothCvAndApplication() {
        when(repo.existsByIntern_IdAndTypeAndStatus(1L, "CV", "APPROVED")).thenReturn(true);
        when(repo.existsByIntern_IdAndTypeAndStatus(1L, "INTERNSHIP_APPLICATION", "APPROVED")).thenReturn(true);

        assertTrue(service.hasAllRequiredDocuments(1L));

        when(repo.existsByIntern_IdAndTypeAndStatus(1L, "INTERNSHIP_APPLICATION", "APPROVED")).thenReturn(false);
        assertFalse(service.hasAllRequiredDocuments(1L));
    }

    @Test
    void getMyDocuments_Success() {
        when(repo.findByIntern_IdOrderByUploadedAtDesc(1L)).thenReturn(Collections.emptyList());

        var result = service.getMyDocuments(1L);

        assertNotNull(result);
        verify(repo).findByIntern_IdOrderByUploadedAtDesc(1L);
    }
}
