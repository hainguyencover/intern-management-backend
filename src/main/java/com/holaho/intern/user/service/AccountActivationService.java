package com.holaho.intern.user.service;

import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.entity.AccountActivationToken;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.AccountActivationTokenRepository;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountActivationService {

    private final AccountActivationTokenRepository activationTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String createActivationToken(User user) {
        activationTokenRepository.deleteByUserId(user.getId());

        String rawToken = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        String tokenHash = hashToken(rawToken);

        AccountActivationToken token = AccountActivationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        activationTokenRepository.save(token);
        log.info("Created activation token for user id: {}, email: {}", user.getId(), user.getEmail());
        return rawToken;
    }

    @Transactional
    public User activateAccount(String rawToken, String newPassword) {
        String tokenHash = hashToken(rawToken);
        AccountActivationToken token = activationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Mã kích hoạt không hợp lệ hoặc không tồn tại"));

        if (token.getUsedAt() != null) {
            throw new IllegalStateException("Mã kích hoạt này đã được sử dụng");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Mã kích hoạt đã hết hạn");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setSecurityVersion((user.getSecurityVersion() != null ? user.getSecurityVersion() : 1) + 1);

        token.setUsedAt(LocalDateTime.now());
        activationTokenRepository.save(token);

        User activatedUser = userRepository.save(user);
        log.info("Successfully activated user account id: {}, email: {}", user.getId(), user.getEmail());
        return activatedUser;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
