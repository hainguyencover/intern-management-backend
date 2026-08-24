package com.holaho.intern.user.service;

import com.holaho.intern.shared.dto.request.CreateUserRequest;
import com.holaho.intern.shared.dto.request.UpdateUserStatusRequest;
import com.holaho.intern.shared.dto.response.UserResponse;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private AccountActivationService accountActivationService;

    @InjectMocks
    private AdminUserService adminUserService;

    private User adminUser;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode("ADMIN");
        adminRole.setName("Administrator");

        adminUser = new User();
        adminUser.setId(10L);
        adminUser.setEmail("admin@example.com");
        adminUser.setFullName("System Admin");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setRoles(Set.of(adminRole));
    }

    @Test
    void createUser_shouldCreateUserWithStatusInvitedAndActivationToken() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("newuser@example.com");
        request.setFullName("New User");
        request.setRoleCodes(List.of("INTERN"));

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(accountActivationService.createActivationToken(any())).thenReturn("token-12345");

        UserResponse response = adminUserService.createUser(request);

        assertThat(response.getEmail()).isEqualTo("newuser@example.com");
        assertThat(response.getStatus()).isEqualTo("INVITED");
        verify(accountActivationService, times(1)).createActivationToken(any());
    }

    @Test
    void updateUserStatus_shouldThrowExceptionWhenDisablingLastAdmin() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.DISABLED);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.updateUserStatus(10L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BR-09");
    }

    @Test
    void deleteUser_shouldSoftDeleteToDisabled() {
        User regularUser = new User();
        regularUser.setId(20L);
        regularUser.setEmail("intern@example.com");
        regularUser.setStatus(UserStatus.ACTIVE);
        regularUser.setRoles(Set.of());

        when(userRepository.findById(20L)).thenReturn(Optional.of(regularUser));

        adminUserService.deleteUser(20L);

        assertThat(regularUser.getStatus()).isEqualTo(UserStatus.DISABLED);
        verify(userRepository, times(1)).save(regularUser);
    }
}
