package com.holaho.intern.user.service;

import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.MentorRepository;
import com.holaho.intern.controller.TaskController;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;


import com.holaho.intern.shared.dto.request.UpdateUserRequest;
import com.holaho.intern.shared.dto.request.CreateUserRequest;
import com.holaho.intern.shared.dto.response.UserResponse;
import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.entity.Mentor;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }

        // Create user
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setStatus(UserStatus.ACTIVE);

        // Assign roles
        Set<Role> roles = new HashSet<>();
        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            for (String roleCode : request.getRoleCodes()) {
                Role role = roleRepository.findByCode(roleCode.toUpperCase())
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));
                roles.add(role);
            }
        } else {
            // Default to INTERN role if no roles specified
            Role internRole = roleRepository.findByCode("INTERN")
                    .orElseThrow(() -> new RuntimeException("INTERN role not found"));
            roles.add(internRole);
        }
        user.setRoles(roles);

        user = userRepository.save(user);
        log.info("Created user: {}", user.getEmail());

        // Auto-create profile based on role
        createProfileForUser(user);

        return mapToResponse(user);
    }

    private void createProfileForUser(User user) {
        boolean isIntern = user.getRoles().stream().anyMatch(r -> "INTERN".equals(r.getCode()));
        boolean isMentor = user.getRoles().stream().anyMatch(r -> "MENTOR".equals(r.getCode()));

        if (isIntern && internProfileRepository.findByUser_Id(user.getId()).isEmpty()) {
            InternProfile profile = new InternProfile();
            profile.setUser(user);
            internProfileRepository.save(profile);
            log.info("Auto-created InternProfile for user: {}", user.getEmail());
        }

        if (isMentor && mentorRepository.findByUser_Id(user.getId()).isEmpty()) {
            Mentor mentor = new Mentor();
            mentor.setUser(user);
            // Try to assign IT department by default
            departmentRepository.findByCode("IT").ifPresent(mentor::setDepartment);
            mentorRepository.save(mentor);
            log.info("Auto-created Mentor profile for user: {}", user.getEmail());
        }
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        user = userRepository.save(user);
        log.info("Updated user: {}", user.getEmail());

        return mapToResponse(user);
    }

    public Page<UserResponse> searchUsers(String email, String fullName, UserStatus status, Pageable pageable) {
        return userRepository.searchUsers(email, fullName, status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(String roleCode) {
        return userRepository.findByRoleCode(roleCode).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        user.setStatus(status);
        user = userRepository.save(user);

        log.info("Updated user {} status to {}", user.getId(), status);
        return mapToResponse(user);
    }

    /**
     * Get intern profile ID by user ID
     * Helper method for TaskController
     */
    @Transactional
    public Long getInternProfileIdByUserId(Long userId) {
        return internProfileRepository.findByUser_Id(userId)
                .map(InternProfile::getId)
                .orElseGet(() -> {
                    // Lazy create if not exists
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("User not found: " + userId));

                    InternProfile newProfile = new InternProfile();
                    newProfile.setUser(user);
                    // Set default empty values if needed
                    newProfile = internProfileRepository.save(newProfile);
                    log.info("Lazily created InternProfile for user ID: {}", userId);
                    return newProfile.getId();
                });
    }

    /**
     * Check if user has intern profile
     */
    @Transactional(readOnly = true)
    public boolean hasInternProfile(Long userId) {
        return internProfileRepository.existsById(userId);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        userRepository.delete(user);
        log.info("Deleted user: {}", id);
    }

    private UserResponse mapToResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .status(String.valueOf(user.getStatus()))
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

}

