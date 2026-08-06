package com.holaho.intern.service;

import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.shared.dto.request.InternProfileRequest;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.mapper.InternMapper;
import com.holaho.intern.repository.*;
import com.holaho.intern.intern.service.InternProfileServiceImpl;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
class InternProfileServiceTest {

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

    private InternProfileRequest request;
    private User user;
    private Role internRole;

    @BeforeEach
    void setUp() {
        request = new InternProfileRequest();
        request.setEmail("intern@example.com");
        request.setFullName("Test Intern");
        request.setStudentCode("INT001");

        user = new User();
        user.setId(1L);
        user.setEmail("intern@example.com");

        internRole = new Role();
        internRole.setCode("INTERN");
    }

    @Test
    void createIntern_WithNewUser_ShouldCreateUserAndProfile() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByCode("INTERN")).thenReturn(Optional.of(internRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(internProfileRepository.existsByUser_Id(anyLong())).thenReturn(false);
        when(internProfileRepository.save(any(InternProfile.class))).thenAnswer(i -> i.getArguments()[0]);

        InternProfileResponse response = internProfileService.createIntern(request);

        verify(userRepository).save(any(User.class));
        verify(internProfileRepository).save(any(InternProfile.class));
    }

    @Test
    void createIntern_WhenProfileExists_ShouldThrowException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(internProfileRepository.existsByUser_Id(1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> internProfileService.createIntern(request));
    }

    @Test
    void getInternProfile_ShouldReturnResponse() {
        InternProfile profile = new InternProfile();
        profile.setId(1L);
        profile.setUser(user);

        when(internProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(internMapper.toResponse(eq(profile), any())).thenReturn(new InternProfileResponse());

        InternProfileResponse response = internProfileService.getInternProfile(1L);

        assertNotNull(response);
        verify(internProfileRepository).findById(1L);
    }
}

