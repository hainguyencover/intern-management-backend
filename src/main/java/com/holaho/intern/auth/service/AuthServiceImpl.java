package com.holaho.intern.auth.service;

import com.holaho.intern.entity.Application;
import com.holaho.intern.shared.dto.request.ChangePasswordRequest;
import com.holaho.intern.shared.dto.request.LoginRequest;
import com.holaho.intern.shared.dto.request.RegisterRequest;
import com.holaho.intern.shared.dto.request.ResetPasswordRequest;
import com.holaho.intern.shared.dto.request.TwoFactorVerifyRequest;
import com.holaho.intern.shared.dto.response.JwtResponse;
import com.holaho.intern.shared.dto.response.TwoFactorResponse;
import com.holaho.intern.shared.dto.response.UserResponse;


import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.entity.RefreshToken;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.exception.ApiException;
import com.holaho.intern.repository.InternProfileRepository;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.repository.RefreshTokenRepository;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.shared.security.JwtTokenProvider;
import com.holaho.intern.shared.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InternProfileRepository internProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(),
            new SystemTimeProvider());

    @Value("${app.jwt.refresh-expiration-ms}")
    private Long refreshExpirationMs;

    @Value("${spring.application.name:InternManagement}")
    private String appName;

    @Override
    @Transactional
    public JwtResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // Check if 2FA is required and verify code
            if (user.getIsTwoFactorEnabled() != null && user.getIsTwoFactorEnabled()) {
                if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isBlank()) {
                    throw new BadRequestException("Mã xác thực 2FA là bắt buộc");
                }
                if (!codeVerifier.isValidCode(user.getTwoFactorSecret(), request.getTwoFactorCode())) {
                    throw new BadRequestException("Mã xác thực 2FA không hợp lệ");
                }
                log.info("2FA verified for user: {}", normalizedEmail);
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            assert userDetails != null;
            String token = tokenProvider.generateToken(userDetails);

            // Extract roles (without ROLE_ prefix)
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                    .collect(Collectors.toSet());

            log.info("User logged in successfully: {}", normalizedEmail);

            RefreshToken refreshToken = createRefreshToken(user.getId());

            return JwtResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken.getToken())
                    .id(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .roles(new ArrayList<>(roles))
                    .build();

        } catch (BadCredentialsException e) {
            log.error("Login failed - Bad credentials for user: {}", normalizedEmail);
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        } catch (LockedException e) {
            log.error("Login failed - Account locked: {}", normalizedEmail);
            throw new BadRequestException("Tài khoản đã bị khóa");
        } catch (DisabledException e) {
            log.error("Login failed - Account disabled: {}", normalizedEmail);
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa");
        }
    }

    @Override
    @Transactional
    public JwtResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email đã tồn tại trong hệ thống");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setStatus(UserStatus.ACTIVE);

        Role internRole = roleRepository.findByCode("INTERN")
                .orElseThrow(() -> new NotFoundException("INTERN role not found"));
        user.setRoles(Collections.singleton(internRole));

        user = userRepository.save(user);

        InternProfile internProfile = new InternProfile();
        internProfile.setUser(user);
        internProfile.setStudentCode(request.getStudentCode());
        internProfile.setUniversity(request.getUniversity());
        internProfile.setMajor(request.getMajor());
        internProfileRepository.save(internProfile);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);

        RefreshToken refreshToken = createRefreshToken(user.getId());

        return JwtResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(Collections.singletonList("INTERN"))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("User not authenticated");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .status(user.getStatus().name())
                .roles(new ArrayList<>(roles))
                .isTwoFactorEnabled(user.getIsTwoFactorEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        if (roles.contains("INTERN")) {
            internProfileRepository.findByUser_Id(user.getId()).ifPresent(ip -> {
                builder.internId(ip.getId());
                applicationRepository.findByIntern_Id(ip.getId())
                        .stream()
                        .max(Comparator.comparing(com.holaho.intern.entity.Application::getAppliedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .ifPresent(app -> builder.applicationStatus(app.getStatus().name()));
            });
        }

        return builder.build();
    }

    @Override
    @Transactional
    public TwoFactorResponse setupTwoFactor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String secret = secretGenerator.generate();
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        dev.samstevens.totp.qr.QrData data = new dev.samstevens.totp.qr.QrData.Builder()
                .label(user.getEmail())
                .issuer(appName)
                .secret(secret)
                .digits(6)
                .period(30)
                .build();

        return new TwoFactorResponse(data.getUri(), secret);
    }

    @Override
    @Transactional
    public void verifyAndEnableTwoFactor(Long userId, TwoFactorVerifyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (codeVerifier.isValidCode(user.getTwoFactorSecret(), request.getCode())) {
            user.setIsTwoFactorEnabled(true);
            userRepository.save(user);
        } else {
            throw new BadRequestException("Mã xác thực không hợp lệ");
        }
    }

    @Override
    @Transactional
    public void disableTwoFactor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setIsTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu cũ không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + normalizedEmail));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.warn("Password reset for user: {} (INSECURE - implement token verification)", normalizedEmail);
    }

    @Override
    @Transactional
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String email = authentication.getName();
            userRepository.findByEmail(email).ifPresent(user -> {
                refreshTokenRepository.deleteByUser(user);
            });
            log.info("User logged out and refresh token purged: {}", email);
        }
        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId).get();
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush(); // Force sync with DB before inserting new token

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Override
    @Transactional
    public JwtResponse refreshToken(String requestRefreshToken) {
        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    CustomUserDetails userDetails = new CustomUserDetails(user);
                    String token = tokenProvider.generateToken(userDetails);
                    RefreshToken newRefreshToken = createRefreshToken(user.getId());

                    List<String> roles = user.getRoles().stream()
                            .map(Role::getCode)
                            .collect(Collectors.toList());

                    return new JwtResponse(token, newRefreshToken.getToken(), user.getId(), user.getEmail(),
                            user.getFullName(), roles);
                })
                .orElseThrow(() -> new NotFoundException("Refresh token is not in database!"));
    }
}
