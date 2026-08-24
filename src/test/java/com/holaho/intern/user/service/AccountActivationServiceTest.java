package com.holaho.intern.user.service;

import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.entity.AccountActivationToken;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.AccountActivationTokenRepository;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountActivationServiceTest {

    @Mock private AccountActivationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountActivationService activationService;

    private User invitedUser;

    @BeforeEach
    void setUp() {
        invitedUser = new User();
        invitedUser.setId(5L);
        invitedUser.setEmail("invited@example.com");
        invitedUser.setStatus(UserStatus.INVITED);
        invitedUser.setSecurityVersion(1);
    }

    @Test
    void createActivationToken_shouldSaveTokenAndReturnRawString() {
        String token = activationService.createActivationToken(invitedUser);

        assertThat(token).isNotBlank();
        verify(tokenRepository, times(1)).save(any(AccountActivationToken.class));
    }

    @Test
    void activateAccount_shouldActivateUserSetPasswordAndUpdateStatus() {
        AccountActivationToken token = AccountActivationToken.builder()
                .id(1L)
                .user(invitedUser)
                .tokenHash("hash123")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("SecretPass123")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User activated = activationService.activateAccount("raw-token", "SecretPass123");

        assertThat(activated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(activated.getEmailVerified()).isTrue();
        assertThat(activated.getSecurityVersion()).isEqualTo(2);
        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void activateAccount_shouldThrowExceptionWhenTokenExpired() {
        AccountActivationToken expiredToken = AccountActivationToken.builder()
                .id(2L)
                .user(invitedUser)
                .tokenHash("hashExpired")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> activationService.activateAccount("raw-expired-token", "SecretPass123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hết hạn");
    }
}
