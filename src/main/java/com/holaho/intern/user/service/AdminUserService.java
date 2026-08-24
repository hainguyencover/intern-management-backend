package com.holaho.intern.user.service;

import com.holaho.intern.entity.Department;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.dto.request.UpdateUserStatusRequest;
import com.holaho.intern.service.EmailService;

import com.holaho.intern.shared.dto.request.CreateUserRequest;
import com.holaho.intern.shared.dto.request.UpdateUserRequest;
import com.holaho.intern.shared.dto.response.UserResponse;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
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
    private final AccountActivationService accountActivationService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email đã tồn tại: " + email);
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(generateRandomPassword()));
        user.setStatus(UserStatus.INVITED);
        user.setSecurityVersion(1);

        Set<Role> roles = new HashSet<>();
        if (request.getRoleCodes() != null) {
            for (String roleCode : request.getRoleCodes()) {
                Role role = roleRepository.findByCode(roleCode)
                        .orElseThrow(() -> new NotFoundException("Role không tồn tại: " + roleCode));
                roles.add(role);
            }
        }
        if (roles.isEmpty()) {
            roleRepository.findByCode("INTERN").ifPresent(roles::add);
        }
        user.setRoles(roles);

        user = userRepository.save(user);
        createProfileForRole(user, request);

        String activationToken = accountActivationService.createActivationToken(user);
        log.info("Created user {} with activation token generated", email);

        try {
            emailService.sendAccountCreatedEmail(user.getEmail(), user.getFullName(), "Kích hoạt tại: /activate?token=" + activationToken);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", email, e.getMessage());
        }

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
                throw new com.holaho.intern.shared.exception.BadRequestException("Trạng thái không hợp lệ: " + status);
            }
        }
        // Use the new repository method that supports filtering by role and keyword
        Page<User> users = userRepository.searchByRoleAndKeyword(role, userStatus, keyword, pageable);
        return users.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + id));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + id));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_ADMIN".equalsIgnoreCase(r.getCode()));
        if (isAdmin && (request.getStatus() == UserStatus.DISABLED || request.getStatus() == UserStatus.LOCKED || request.getStatus() == UserStatus.SUSPENDED)) {
            if (userRepository.countActiveAdmins() <= 1) {
                throw new ConflictException("Không thể vô hiệu hóa tài khoản Quản trị viên (ADMIN) duy nhất còn lại trong hệ thống (BR-09)");
            }
        }

        user.setStatus(request.getStatus());
        user.setSecurityVersion((user.getSecurityVersion() != null ? user.getSecurityVersion() : 1) + 1);
        userRepository.save(user);
        log.info("Updated user {} status to {}", user.getEmail(), request.getStatus());

        return mapToResponse(user);
    }

    @Transactional
    public void assignRoles(Long id, List<String> roleCodes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + id));

        boolean isAdminCurrently = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_ADMIN".equalsIgnoreCase(r.getCode()));
        boolean willBeAdmin = roleCodes.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r));

        if (isAdminCurrently && !willBeAdmin && userRepository.countActiveAdmins() <= 1) {
            throw new ConflictException("Không thể gỡ vai trò Quản trị viên (ADMIN) duy nhất còn lại trong hệ thống (BR-09)");
        }

        Set<Role> roles = new HashSet<>();
        for (String roleCode : roleCodes) {
            Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new NotFoundException("Role không tồn tại: " + roleCode));
            roles.add(role);
        }

        user.setRoles(roles);
        user.setSecurityVersion((user.getSecurityVersion() != null ? user.getSecurityVersion() : 1) + 1);
        userRepository.save(user);

        // Auto-create profiles if needed
        boolean isMentor = roles.stream().anyMatch(r -> "MENTOR".equalsIgnoreCase(r.getCode()));
        if (isMentor && !mentorRepository.existsByUser_Id(user.getId())) {
            Mentor mentor = new Mentor();
            mentor.setUser(user);
            mentor.setTitle("Mentor");
            mentorRepository.save(mentor);
            log.info("Auto-created Mentor profile for user {}", user.getEmail());
        }

        log.info("Assigned roles {} to user {}", roleCodes, user.getEmail());
    }

    @Transactional
    public void toggleUserLock(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + id));

        if (user.getStatus() == UserStatus.ACTIVE) {
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_ADMIN".equalsIgnoreCase(r.getCode()));
            if (isAdmin && userRepository.countActiveAdmins() <= 1) {
                throw new ConflictException("Không thể khóa tài khoản Quản trị viên (ADMIN) duy nhất còn lại trong hệ thống (BR-09)");
            }
            user.setStatus(UserStatus.LOCKED);
            log.info("Locked user {}", user.getEmail());
        } else if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
            log.info("Unlocked user {}", user.getEmail());
        }

        user.setSecurityVersion((user.getSecurityVersion() != null ? user.getSecurityVersion() : 1) + 1);
        userRepository.save(user);
    }

    @Transactional
    public String resetUserPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + id));

        String newPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setSecurityVersion((user.getSecurityVersion() != null ? user.getSecurityVersion() : 1) + 1);
        userRepository.save(user);

        log.info("Reset password for user {}, new password: {}", user.getEmail(), newPassword);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), newPassword);

        return newPassword;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + id));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_ADMIN".equalsIgnoreCase(r.getCode()));
        if (isAdmin && userRepository.countActiveAdmins() <= 1) {
            throw new ConflictException("Không thể vô hiệu hóa tài khoản Quản trị viên (ADMIN) duy nhất còn lại trong hệ thống (BR-09)");
        }

        user.setStatus(UserStatus.DISABLED);
        user.setSecurityVersion((user.getSecurityVersion() != null ? user.getSecurityVersion() : 1) + 1);
        userRepository.save(user);
        log.info("Soft-deleted (disabled) user: {}", id);
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

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + id));

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
        log.info("Updated user info for user: {}", user.getEmail());

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public byte[] exportUsersToExcel() throws java.io.IOException {
        List<User> users = userRepository.findAll();
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Users");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Email");
            header.createCell(2).setCellValue("Full Name");
            header.createCell(3).setCellValue("Phone");
            header.createCell(4).setCellValue("Status");
            header.createCell(5).setCellValue("Roles");
            header.createCell(6).setCellValue("Created At");

            int rowIdx = 1;
            for (User user : users) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getEmail());
                row.createCell(2).setCellValue(user.getFullName() != null ? user.getFullName() : "");
                row.createCell(3).setCellValue(user.getPhone() != null ? user.getPhone() : "");
                row.createCell(4).setCellValue(user.getStatus() != null ? user.getStatus().name() : "");
                row.createCell(5).setCellValue(user.getRoles().stream().map(Role::getCode).collect(Collectors.joining(",")));
                row.createCell(6).setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public int importUsersFromExcel(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        int count = 0;
        try (java.io.InputStream is = file.getInputStream();
             org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(is)) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            for (int i = 1; i < rowCount; i++) { // Skip header row
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;

                org.apache.poi.ss.usermodel.Cell emailCell = row.getCell(0);
                if (emailCell == null) continue;
                String email = emailCell.getStringCellValue().trim().toLowerCase();
                if (email.isBlank()) continue;

                if (userRepository.existsByEmail(email)) {
                    log.warn("Skip importing user {} - email already exists", email);
                    continue;
                }

                String fullName = row.getCell(1) != null ? row.getCell(1).getStringCellValue().trim() : "";
                String phone = row.getCell(2) != null ? row.getCell(2).getStringCellValue().trim() : "";
                String rolesStr = row.getCell(3) != null ? row.getCell(3).getStringCellValue().trim() : "INTERN";

                // Generate random password
                String randomPassword = generateRandomPassword();

                User user = new User();
                user.setEmail(email);
                user.setFullName(fullName);
                user.setPhone(phone);
                user.setPasswordHash(passwordEncoder.encode(randomPassword));
                user.setStatus(UserStatus.ACTIVE);

                Set<Role> roles = new HashSet<>();
                for (String roleCode : rolesStr.split(",")) {
                    roleRepository.findByCode(roleCode.trim().toUpperCase()).ifPresent(roles::add);
                }
                if (roles.isEmpty()) {
                    roleRepository.findByCode("INTERN").ifPresent(roles::add);
                }
                user.setRoles(roles);

                user = userRepository.save(user);

                // Optional profile creation based on columns if present
                CreateUserRequest request = new CreateUserRequest();
                if (row.getCell(4) != null) request.setStudentCode(row.getCell(4).getStringCellValue().trim());
                if (row.getCell(5) != null) request.setUniversity(row.getCell(5).getStringCellValue().trim());
                if (row.getCell(6) != null) request.setMajor(row.getCell(6).getStringCellValue().trim());
                if (row.getCell(7) != null) request.setTitle(row.getCell(7).getStringCellValue().trim());

                createProfileForRole(user, request);

                emailService.sendAccountCreatedEmail(user.getEmail(), user.getFullName(), randomPassword);
                count++;
            }
        }
        return count;
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
