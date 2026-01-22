package com.example.backend.service;

import com.example.backend.entity.Mentor;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.InternProfile;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.MentorRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class InternProfileServiceTest {

    @Mock
    private InternProfileRepository internProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MentorRepository mentorRepository;

    @InjectMocks
    private InternProfileService internProfileService;

    private User mentorUser;
    private InternProfile internProfile;

    @BeforeEach
    void setUp() {
        mentorUser = new User();
        mentorUser.setId(3L);
        mentorUser.setEmail("mentor@example.com");

        internProfile = new InternProfile();
        internProfile.setId(1L);
    }

    @Test
    void assignMentorByUserId_UserHasMentorRole_ShouldAutoCreateProfile() {
        // Arrange
        Role mentorRole = new Role();
        mentorRole.setCode("MENTOR");
        mentorUser.setRoles(Set.of(mentorRole));

        Mockito.when(internProfileRepository.findById(1L)).thenReturn(Optional.of(internProfile));
        Mockito.when(mentorRepository.findByUser_Id(3L)).thenReturn(Optional.empty()); // No existing profile
        Mockito.when(userRepository.findById(3L)).thenReturn(Optional.of(mentorUser));
        Mockito.when(mentorRepository.save(any(Mentor.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        internProfileService.assignMentorByUserId(1L, 3L);

        // Assert
        Mockito.verify(mentorRepository).save(any(Mentor.class));
        Mockito.verify(internProfileRepository).save(internProfile);
        Assertions.assertNotNull(internProfile.getMentor());
    }

    @Test
    void assignMentorByUserId_UserHasRoleMentorPrefix_ShouldAutoCreateProfile() {
        // Arrange
        Role mentorRole = new Role();
        mentorRole.setCode("ROLE_MENTOR");
        mentorUser.setRoles(Set.of(mentorRole));

        Mockito.when(internProfileRepository.findById(1L)).thenReturn(Optional.of(internProfile));
        Mockito.when(mentorRepository.findByUser_Id(3L)).thenReturn(Optional.empty());
        Mockito.when(userRepository.findById(3L)).thenReturn(Optional.of(mentorUser));
        Mockito.when(mentorRepository.save(any(Mentor.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        internProfileService.assignMentorByUserId(1L, 3L);

        // Assert
        Mockito.verify(mentorRepository).save(any(Mentor.class));
    }

    @Test
    void assignMentorByUserId_UserHasNoMentorRole_ShouldThrowException() {
        // Arrange
        Role userRole = new Role();
        userRole.setCode("USER");
        mentorUser.setRoles(Set.of(userRole));

        Mockito.when(internProfileRepository.findById(1L)).thenReturn(Optional.of(internProfile));
        Mockito.when(mentorRepository.findByUser_Id(3L)).thenReturn(Optional.empty());
        Mockito.when(userRepository.findById(3L)).thenReturn(Optional.of(mentorUser));

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            internProfileService.assignMentorByUserId(1L, 3L);
        });

        // Verify message contains debug info
        Assertions.assertTrue(exception.getMessage().contains("does not have MENTOR role"));
        Assertions.assertTrue(exception.getMessage().contains("USER"));
    }
}
