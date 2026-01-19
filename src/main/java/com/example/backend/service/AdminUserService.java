package com.example.backend.service;

import com.example.backend.dto.request.CreateUserRequest;
import com.example.backend.dto.response.UserResponse;
import com.example.backend.entity.*;
import com.example.backend.enums.UserStatus;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // Validate email unique
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        // Use custom password if provided, otherwise generate random
        String passwordToUse;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            passwordToUse = request.getPassword();
        } else {
            passwordToUse = generateRandomPassword();
        }

        // Create user
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(passwordToUse));
        user.setStatus(UserStatus.ACTIVE);

        // Assign roles
        Set<Role> roles = new HashSet<>();
        if (request.getRoleCodes() != null) {
            for (String roleCode : request.getRoleCodes()) {
                Role role = roleRepository.findByCode(roleCode)
                        .orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + roleCode));
                roles.add(role);
            }
        }
        user.setRoles(roles);

        user = userRepository.save(user);

        // Create profile based on role
        createProfileForRole(user, request);

        log.info("Created user {} with password provided: {}", email,
                (request.getPassword() != null ? "YES" : "NO - Generated: " + passwordToUse));
        // TODO: Send email with temp password

        return mapToResponse(user);
    }

    private void createProfileForRole(User user, CreateUserRequest request) {
        boolean isIntern = user.getRoles().stream()
                .anyMatch(r -> "INTERN".equalsIgnoreCase(r.getCode()));
        boolean isMentor = user.getRoles().stream()
                .anyMatch(r -> "MENTOR".equalsIgnoreCase(r.getCode()));

        if (isIntern) {
            InternProfile profile = new InternProfile();
            profile.setUser(user);
            profile.setStudentCode(request.getStudentCode());
            profile.setUniversity(request.getUniversity());
            profile.setMajor(request.getMajor());
            internProfileRepository.save(profile);
        }

        if (isMentor) {
            Mentor mentor = new Mentor();
            mentor.setUser(user);
            mentor.setTitle(request.getTitle());
            if (request.getDepartmentId() != null) {
                Department dept = departmentRepository.findById(request.getDepartmentId()).orElse(null);
                mentor.setDepartment(dept);
            }
            mentorRepository.save(mentor);
        }
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String role, String status, String keyword, Pageable pageable) {
        UserStatus userStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                userStatus = UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid status or handle it
            }
        }
        // Use the new repository method that supports filtering by role and keyword
        Page<User> users = userRepository.searchByRoleAndKeyword(role, userStatus, keyword, pageable);
        return users.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, com.example.backend.dto.request.UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        user.setStatus(request.getStatus());
        userRepository.save(user);
        log.info("Updated user {} status to {}", user.getEmail(), request.getStatus());

        return mapToResponse(user);
    }

    @Transactional
    public void assignRoles(Long id, List<String> roleCodes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        Set<Role> roles = new HashSet<>();
        for (String roleCode : roleCodes) {
            Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + roleCode));
            roles.add(role);
        }

        user.setRoles(roles);
        userRepository.save(user);
        log.info("Assigned roles {} to user {}", roleCodes, user.getEmail());
    }

    @Transactional
    public void toggleUserLock(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.LOCKED);
            log.info("Locked user {}", user.getEmail());
        } else if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
            log.info("Unlocked user {}", user.getEmail());
        }

        userRepository.save(user);
    }

    @Transactional
    public String resetUserPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        String newPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Reset password for user {}, new password: {}", user.getEmail(), newPassword);
        // TODO: Send email with new password

        return newPassword;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        userRepository.delete(user);
        log.info("Deleted user: {}", id);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus().name());
        response.setRoles(user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toList()));
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
