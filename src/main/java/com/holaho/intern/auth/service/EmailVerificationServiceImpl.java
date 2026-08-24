package com.holaho.intern.auth.service;

import com.holaho.intern.auth.entity.EmailVerificationToken;
import com.holaho.intern.auth.repository.EmailVerificationTokenRepository;
import com.holaho.intern.service.EmailService;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final long EXPIRATION_HOURS = 24;

    @Override
    @Transactional
    public String createVerificationToken(User user) {
        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(EXPIRATION_HOURS))
                .build();

        tokenRepository.save(token);
        log.info("========== [DEV] EMAIL VERIFICATION TOKEN ==========");
        log.info("User: {} | Token: {}", user.getEmail(), rawToken);
        log.info("====================================================");
        return rawToken;
    }

    @Override
    @Transactional
    public void verifyEmail(String rawToken) {
        String tokenHash = hashToken(rawToken);
        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Token xác thực không hợp lệ"));

        if (token.getVerifiedAt() != null) {
            throw new BadRequestException("Email đã được xác thực trước đó");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token xác thực đã hết hạn. Vui lòng yêu cầu gửi lại token mới");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setVerifiedAt(LocalDateTime.now());
        tokenRepository.save(token);

        log.info("Successfully verified email for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với email: " + normalizedEmail));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email của bạn đã được xác thực");
        }

        String rawToken = createVerificationToken(user);
        emailService.sendEmailVerificationToken(user.getEmail(), user.getFullName(), rawToken);
        log.info("Resent email verification token to user: {}", normalizedEmail);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing verification token", e);
        }
    }
}
