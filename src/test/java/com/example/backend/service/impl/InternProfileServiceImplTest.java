package com.example.backend.service.impl;

import com.example.backend.dto.request.InternProfileRequest;
import com.example.backend.dto.response.InternProfileResponse;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.mapper.InternMapper;
import com.example.backend.repository.*;
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
    private com.example.backend.elasticsearch.service.SearchService searchService;
    @Mock
    private com.example.backend.service.AiService aiService;

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
    void createIntern_NewUser_Success() {
        // Arrange
        InternProfileRequest request = new InternProfileRequest();
        request.setEmail("new_intern@example.com");
        request.setFullName("New Intern");
        request.setStudentCode("ST002");

        Role internRole = new Role();
        internRole.setCode("INTERN");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setEmail("new_intern@example.com");

        when(userRepository.findByEmail("new_intern@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByCode("INTERN")).thenReturn(Optional.of(internRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(internProfileRepository.existsByUser_Id(2L)).thenReturn(false);
        when(internProfileRepository.save(any(InternProfile.class))).thenAnswer(i -> i.getArguments()[0]);
        when(internMapper.toResponse(any(), any())).thenReturn(new InternProfileResponse());

        // Act
        InternProfileResponse response = internProfileService.createIntern(request);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(internProfileRepository).save(any(InternProfile.class));
    }

    @Test
    void getInternProfile_NotFound_ThrowsException() {
        // Arrange
        when(internProfileRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(com.example.backend.exception.ResourceNotFoundException.class,
                () -> internProfileService.getInternProfile(1L));
    }
}
