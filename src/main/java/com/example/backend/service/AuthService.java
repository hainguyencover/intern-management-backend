package com.example.backend.service;

import com.example.backend.dto.request.ChangePasswordRequest;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.dto.request.ResetPasswordRequest;
import com.example.backend.dto.response.JwtResponse;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.enums.UserStatus;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InternProfileRepository internProfileRepository;
    private final com.example.backend.repository.ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    /**
     * Login user
     */
    @Transactional
    public JwtResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            assert userDetails != null;
            String token = tokenProvider.generateToken(userDetails);

            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // Extract roles (without ROLE_ prefix)
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                    .collect(Collectors.toSet());

            log.info("User logged in successfully: {}", normalizedEmail);

            return JwtResponse.builder()
                    .token(token)
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

    /**
     * Register new user (INTERN role by default)
     */
    @Transactional
    public JwtResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        log.info("Registration attempt for email: {}", normalizedEmail);

        // Check if email already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email đã tồn tại trong hệ thống");
        }

        // Create User
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setStatus(UserStatus.ACTIVE);

        // Assign INTERN role by default
        Role internRole = roleRepository.findByCode("INTERN")
                .orElseThrow(() -> new NotFoundException("INTERN role not found in system"));

        Set<Role> roles = new HashSet<>();
        roles.add(internRole);
        user.setRoles(roles);

        user = userRepository.save(user);
        log.info("User created successfully: {}", normalizedEmail);

        // Auto-create InternProfile for INTERN role
        InternProfile internProfile = new InternProfile();
        internProfile.setUser(user);
        internProfileRepository.save(internProfile);
        log.info("InternProfile auto-created for user: {}", user.getId());

        // Auto login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);

        // Extract roles
        Set<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        log.info("User registered and logged in successfully: {}", normalizedEmail);

        return JwtResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(new ArrayList<>(roleCodes))
                .build();
    }

    /**
     * Get current logged-in user info
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        // Extract roles
        Set<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        var builder = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .status(user.getStatus().name())
                .roles(new ArrayList<>(roles))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        // US10: Populate intern info if user is INTERN
        if (roles.contains("INTERN")) {
            internProfileRepository.findByUser_Id(user.getId()).ifPresent(ip -> {
                builder.internId(ip.getId());

                // Find latest application status
                List<com.example.backend.entity.Application> apps = applicationRepository.findByIntern_Id(ip.getId());
                if (!apps.isEmpty()) {
                    // Sort by appliedAt desc or ID desc to get latest
                    apps.sort((a1, a2) -> {
                        if (a1.getAppliedAt() == null || a2.getAppliedAt() == null)
                            return 0;
                        return a2.getAppliedAt().compareTo(a1.getAppliedAt());
                    });
                    builder.applicationStatus(apps.get(0).getStatus().name());
                }
            });
        }

        return builder.build();
    }

    /**
     * Change password (for logged-in user)
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu cũ không đúng");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", email);
    }

    /**
     * Reset password (for forgot password flow)
     * Note: This is simplified version. Production should use email verification
     * token
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + normalizedEmail));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.warn("Password reset for user: {} (INSECURE - implement token verification)", normalizedEmail);
    }

    /**
     * Logout (client-side only - invalidate token on client)
     * Server-side token invalidation requires token blacklist/Redis
     */
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String email = authentication.getName();
            log.info("User logged out: {}", email);
        }
        SecurityContextHolder.clearContext();
    }
}
