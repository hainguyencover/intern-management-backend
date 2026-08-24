package com.holaho.intern.service.impl;

import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.intern.service.InternProfileServiceImpl;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.shared.dto.request.InternProfileRequest;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.mapper.InternMapper;
import com.holaho.intern.repository.*;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternProfileServiceImplTest {

    @Mock
    private InternProfileRepository internProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MentorRepository mentorRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private InternMapper internMapper;
    @Mock
    private com.holaho.intern.shared.elasticsearch.service.SearchService searchService;
    @Mock
    private com.holaho.intern.service.AiService aiService;

    @Mock
    private com.holaho.intern.service.AuditLogService auditLogService;

    @InjectMocks
    private InternProfileServiceImpl internProfileService;

    @Test
    void createIntern_WithExistingUser_Success() {
        // Arrange
        InternProfileRequest request = new InternProfileRequest();
        request.setUserId(1L);
        request.setStudentCode("ST001");

        User user = new User();
        user.setId(1L);
        user.setEmail("intern@example.com");

        InternProfile savedProfile = new InternProfile();
        savedProfile.setId(10L);
        savedProfile.setUser(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(internProfileRepository.existsByUser_Id(1L)).thenReturn(false);
        when(internProfileRepository.save(any(InternProfile.class))).thenReturn(savedProfile);
        when(internMapper.toResponse(any(), any())).thenReturn(new InternProfileResponse());

        // Act
        InternProfileResponse response = internProfileService.createIntern(request);

        // Assert
        assertNotNull(response);
        verify(internProfileRepository).save(any(InternProfile.class));
    }

    @Test
    void createIntern_UserNotFound_ThrowsBadRequestExceptionForBR09() {
        // Arrange
        InternProfileRequest request = new InternProfileRequest();
        // No userId or email provided to trigger BadRequestException

        // Act & Assert
        assertThrows(com.holaho.intern.shared.exception.BadRequestException.class,
                () -> internProfileService.createIntern(request));
    }

    @Test
    void updateIntern_StatusCompleted_ThrowsConflictException() {
        // Arrange
        InternProfile profile = new InternProfile();
        profile.setStatus("COMPLETED");

        when(internProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        InternProfileRequest request = new InternProfileRequest();
        request.setUniversity("HUST");

        // Act & Assert
        assertThrows(com.holaho.intern.shared.exception.ConflictException.class,
                () -> internProfileService.updateIntern(1L, request));
    }

    @Test
    void updateIntern_StatusRejected_ThrowsConflictException() {
        // Arrange
        InternProfile profile = new InternProfile();
        profile.setStatus("REJECTED");

        when(internProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        InternProfileRequest request = new InternProfileRequest();
        request.setUniversity("HUST");

        // Act & Assert
        assertThrows(com.holaho.intern.shared.exception.ConflictException.class,
                () -> internProfileService.updateIntern(1L, request));
    }

    @Test
    void updateIntern_ValidProfile_Success() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("intern@example.com");

        InternProfile profile = new InternProfile();
        profile.setId(1L);
        profile.setUser(user);
        profile.setStatus("INTERNING");

        when(internProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(internProfileRepository.save(any(InternProfile.class))).thenReturn(profile);
        when(internMapper.toResponse(any(), any())).thenReturn(new InternProfileResponse());

        InternProfileRequest request = new InternProfileRequest();
        request.setUniversity("Đại học Bách Khoa");
        request.setMajor("Công nghệ Thông tin");

        // Act
        InternProfileResponse response = internProfileService.updateIntern(1L, request);

        // Assert
        assertNotNull(response);
        verify(internProfileRepository).save(profile);
        verify(auditLogService).createAuditLog(any(), anyString(), eq("UPDATE"), eq("INTERN_PROFILE"), eq(1L), eq("SUCCESS"), anyString(), any(), any(), any(), any());
    }

    @Test
    void getInternProfile_NotFound_ThrowsException() {
        // Arrange
        when(internProfileRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(com.holaho.intern.shared.exception.NotFoundException.class,
                () -> internProfileService.getInternProfile(1L));
    }
}
