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
        assertThrows(com.holaho.intern.shared.exception.NotFoundException.class,
                () -> internProfileService.getInternProfile(1L));
    }
}
