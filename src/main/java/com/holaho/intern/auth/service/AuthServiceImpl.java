package com.holaho.intern.auth.service;

import com.holaho.intern.shared.dto.request.ChangePasswordRequest;
import com.holaho.intern.shared.dto.request.LoginRequest;
import com.holaho.intern.shared.dto.request.RegisterRequest;
import com.holaho.intern.shared.dto.request.ResetPasswordRequest;
import com.holaho.intern.shared.dto.request.TwoFactorVerifyRequest;
import com.holaho.intern.shared.dto.response.JwtResponse;
import com.holaho.intern.shared.dto.response.TwoFactorResponse;
import com.holaho.intern.shared.dto.response.UserResponse;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.entity.RefreshToken;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.exception.ApiException;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.repository.RefreshTokenRepository;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.shared.security.JwtTokenProvider;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.shared.annotation.Auditable;
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
    private final com.holaho.intern.user.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    private final com.holaho.intern.service.EmailService emailService;
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
    @Auditable(action = "USER_LOGIN")
    public JwtResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // Check if account is locked
            if (user.getLockTime() != null && user.getLockTime().isAfter(java.time.Instant.now())) {
                long minutesLeft = java.time.Duration.between(java.time.Instant.now(), user.getLockTime()).toMinutes() + 1;
                throw new BadRequestException("Tài khoản đã bị khoá tạm thời do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + minutesLeft + " phút.");
            }

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

            // Reset failed attempts upon successful login
            if (user.getFailedAttempts() > 0 || user.getLockTime() != null) {
                user.setFailedAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            }

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
            userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
                int attempts = user.getFailedAttempts() + 1;
                user.setFailedAttempts(attempts);
                if (attempts >= 5) {
                    user.setLockTime(java.time.Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES));
                    log.warn("Account locked for user: {} due to 5 failed attempts", normalizedEmail);
                }
                userRepository.save(user);
            });
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        } catch (LockedException e) {
            log.error("Login failed - Account locked: {}", normalizedEmail);
            throw new BadRequestException("Tài khoản đã bị khóa");
        } catch (DisabledException e) {
            log.error("Login failed - Account disabled: {}", normalizedEmail);
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa");
        }
    }

    private final EmailVerificationService emailVerificationService;

    @Override
    @Transactional
    @Auditable(action = "USER_REGISTER")
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
        user.setEmailVerified(false);

        Role internRole = roleRepository.findByCode("INTERN")
                .orElseThrow(() -> new NotFoundException("INTERN role not found"));
        user.setRoles(Collections.singleton(internRole));

        user = userRepository.save(user);

        InternProfile internProfile = new InternProfile();
        internProfile.setUser(user);
        internProfile.setStudentCode(request.getStudentCode());
        internProfile.setUniversity(request.getUniversity());
        internProfile.setMajor(request.getMajor());
        internProfile.setStatus("DRAFT");
        internProfileRepository.save(internProfile);

        // Issue email verification token & dispatch email
        String verificationToken = emailVerificationService.createVerificationToken(user);
        emailService.sendEmailVerificationToken(user.getEmail(), user.getFullName(), verificationToken);

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
    @Transactional
    public void verifyEmail(com.holaho.intern.auth.dto.request.VerifyEmailRequest request) {
        emailVerificationService.verifyEmail(request.getToken());
    }

    @Override
    @Transactional
    public void resendVerification(com.holaho.intern.auth.dto.request.ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");
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
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
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
    @Auditable(action = "CHANGE_PASSWORD")
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
    public void forgotPassword(com.holaho.intern.shared.dto.request.ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với email: " + normalizedEmail));

        passwordResetTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.flush();

        com.holaho.intern.user.entity.PasswordResetToken resetToken = new com.holaho.intern.user.entity.PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiryDate(Instant.now().plus(24, java.time.temporal.ChronoUnit.HOURS));

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken.getToken());

        log.info("Password reset email sent for user: {}", normalizedEmail);
    }

    @Override
    @Transactional
    @Auditable(action = "RESET_PASSWORD")
    public void resetPassword(ResetPasswordRequest request) {
        com.holaho.intern.user.entity.PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Token đặt lại mật khẩu không hợp lệ (không tìm thấy)"));

        if (resetToken.isExpired()) {
            throw new BadRequestException("Token đặt lại mật khẩu đã hết hạn");
        }

        if (resetToken.isUsed()) {
            throw new BadRequestException("Token đặt lại mật khẩu đã được sử dụng");
        }

        User user = resetToken.getUser();

        if (request.getEmail() != null && !request.getEmail().trim().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("Token đặt lại mật khẩu không hợp lệ (email không khớp)");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    @Auditable(action = "USER_LOGOUT")
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
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    private RefreshToken createRefreshTokenWithoutDeleting(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public JwtResponse refreshToken(String requestRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new NotFoundException("Refresh token is not in database!"));

        // Replay Attack Detection
        if (token.isUsed() || token.isRevoked()) {
            User user = token.getUser();
            refreshTokenRepository.deleteByUser(user); // Revoke all active sessions for this user
            log.warn("Security Alert: Replay attack detected for user: {}. Revoking all sessions.", user.getEmail());
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Cảnh báo bảo mật: Token này đã được sử dụng. Tất cả các phiên đăng nhập khác của bạn đã bị thu hồi.");
        }

        // Verify expiration
        verifyExpiration(token);

        User user = token.getUser();

        // Mark old token as used
        token.setUsed(true);
        refreshTokenRepository.save(token);

        // Generate new token pair
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = tokenProvider.generateToken(userDetails);

        // Create new Refresh Token without deleting history
        RefreshToken newRefreshToken = createRefreshTokenWithoutDeleting(user);

        // Set replacement tracking
        token.setReplacedByToken(newRefreshToken.getToken());
        refreshTokenRepository.save(token);

        List<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toList());

        return new JwtResponse(newAccessToken, newRefreshToken.getToken(), user.getId(), user.getEmail(),
                user.getFullName(), roles);
    }
}
