package com.example.backend.service.impl;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.response.JwtResponse;
import com.example.backend.entity.RefreshToken;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.ApplicationRepository;
import com.example.backend.repository.RefreshTokenRepository;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private InternProfileRepository internProfileRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 3600000L);
    }

    @Test
    void login_Success() {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFullName("Test User");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(tokenProvider.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERN"))).when(authentication)
                .getAuthorities();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        JwtResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("INTERN"));
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void login_BadCredentials() {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("wrong")
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password")
                .build();
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    void register_Success() {
        // Arrange
        com.example.backend.dto.request.RegisterRequest registerRequest = new com.example.backend.dto.request.RegisterRequest();
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password");
        registerRequest.setFullName("New User");

        Role internRole = new Role();
        internRole.setCode("INTERN");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setEmail("new@example.com");
        savedUser.setFullName("New User");
        savedUser.setRoles(Collections.singleton(internRole));

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("new-refresh-token");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByCode("INTERN")).thenReturn(Optional.of(internRole));
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(tokenProvider.generateToken(userDetails)).thenReturn("new-jwt-token");
        when(userRepository.findById(2L)).thenReturn(Optional.of(savedUser));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        JwtResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        assertEquals("new@example.com", response.getEmail());
        verify(internProfileRepository).save(any());
    }

    @Test
    void register_Conflict() {
        // Arrange
        com.example.backend.dto.request.RegisterRequest registerRequest = new com.example.backend.dto.request.RegisterRequest();
        registerRequest.setEmail("existing@example.com");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(com.example.backend.exception.ConflictException.class,
                () -> authService.register(registerRequest));
    }

    @Test
    void refreshToken_Success() {
        // Arrange
        String oldRefreshToken = "old-token";
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRoles(Collections.emptySet());

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setToken(oldRefreshToken);
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setExpiryDate(Instant.now().plusSeconds(3600));

        RefreshToken newRefreshTokenEntity = new RefreshToken();
        newRefreshTokenEntity.setToken("new-token");

        when(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(Optional.of(refreshTokenEntity));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(any())).thenReturn("new-jwt");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newRefreshTokenEntity);

        // Act
        JwtResponse response = authService.refreshToken(oldRefreshToken);

        // Assert
        assertNotNull(response);
        assertEquals("new-jwt", response.getToken());
        assertEquals("new-token", response.getRefreshToken());
    }

    @Test
    void changePassword_Success() {
        // Arrange
        com.example.backend.dto.request.ChangePasswordRequest request = new com.example.backend.dto.request.ChangePasswordRequest();
        request.setOldPassword("old");
        request.setNewPassword("new");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded-old");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("encoded-new");

        // Act
        authService.changePassword(request);

        // Assert
        assertEquals("encoded-new", user.getPasswordHash());
        verify(userRepository).save(user);
    }
}
