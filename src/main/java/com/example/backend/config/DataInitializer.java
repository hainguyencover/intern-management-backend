package com.example.backend.config;

import com.example.backend.entity.*;
import com.example.backend.enums.UserStatus;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting Data Initialization...");

        Role adminRole = createRoleIfNotExists("ADMIN", "Administrator");
        Role hrRole = createRoleIfNotExists("HR", "Human Resources");
        Role mentorRole = createRoleIfNotExists("MENTOR", "Mentor");
        Role internRole = createRoleIfNotExists("INTERN", "Intern");

        // Seed Permissions
        createPermissionIfNotExists("USER_CREATE", "Create Users");
        createPermissionIfNotExists("USER_READ", "View Users");
        createPermissionIfNotExists("USER_UPDATE", "Update Users");
        createPermissionIfNotExists("USER_LOCK", "Lock/Unlock Users");
        createPermissionIfNotExists("USER_RESET_PASSWORD", "Reset User Password");
        createPermissionIfNotExists("ROLE_READ", "View Roles");
        createPermissionIfNotExists("ROLE_UPDATE", "Update Roles");
        createPermissionIfNotExists("PERMISSION_READ", "View Permissions");
        createPermissionIfNotExists("PERMISSION_UPDATE", "Update Permissions");
        createPermissionIfNotExists("BACKUP_RUN", "Run System Backup");
        createPermissionIfNotExists("BACKUP_READ", "View Backups");
        createPermissionIfNotExists("AUDIT_READ", "View Audit Logs");

        // Assign Permissions to Roles (Basic setup)
        assignPermissionToRole(adminRole, "USER_CREATE");
        assignPermissionToRole(adminRole, "USER_READ");
        assignPermissionToRole(adminRole, "USER_UPDATE");
        assignPermissionToRole(adminRole, "USER_LOCK");
        assignPermissionToRole(adminRole, "USER_RESET_PASSWORD");
        assignPermissionToRole(adminRole, "ROLE_READ");
        assignPermissionToRole(adminRole, "ROLE_UPDATE");
        assignPermissionToRole(adminRole, "PERMISSION_READ");
        assignPermissionToRole(adminRole, "PERMISSION_UPDATE");
        assignPermissionToRole(adminRole, "BACKUP_RUN");
        assignPermissionToRole(adminRole, "BACKUP_READ");
        assignPermissionToRole(adminRole, "AUDIT_READ");

        // HR Permissions (Subset)
        assignPermissionToRole(hrRole, "USER_READ");
        assignPermissionToRole(hrRole, "USER_CREATE"); // HR can create interns/mentors maybe?

        // Ensure at least one Department exists
        createDepartmentIfNotExists("IT", "Information Technology", "IT Department");

        createUserIfNotExists("admin@company.com", "System Admin", "admin123", Set.of(adminRole));
        createUserIfNotExists("hr@company.com", "HR Manager", "hr123", Set.of(hrRole));
        createUserIfNotExists("intern@student.com", "Intern Demo", "intern123", Set.of(internRole));
        createUserIfNotExists("mentor1@company.com", "Mentor One", "mentor123", Set.of(mentorRole));
        createUserIfNotExists("mentor2@company.com", "Mentor Two", "mentor123", Set.of(mentorRole));

        log.info("Data Initialization Complete!");
        log.info("Test credentials:");
        log.info("  Admin: admin@company.com / admin123");
        log.info("  HR:    hr@company.com / hr123");

        log.info("  Intern: intern@student.com / intern123");
        log.info("  Mentor 1: mentor1@company.com / mentor123");
        log.info("  Mentor 2: mentor2@company.com / mentor123");
    }

    private Role createRoleIfNotExists(String code, String name) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setCode(code);
                    r.setName(name);
                    return roleRepository.save(r);
                });
    }

    private Permission createPermissionIfNotExists(String code, String name) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> {
                    Permission p = new Permission();
                    p.setCode(code);
                    p.setName(name);
                    return permissionRepository.save(p);
                });
    }

    private void assignPermissionToRole(Role role, String permissionCode) {
        permissionRepository.findByCode(permissionCode).ifPresent(p -> {
            if (role.getPermissions().stream().noneMatch(rp -> rp.getCode().equals(permissionCode))) {
                role.getPermissions().add(p);
                roleRepository.save(role);
            }
        });
    }

    private void createDepartmentIfNotExists(String code, String name, String desc) {
        if (departmentRepository.findByCode(code).isEmpty()) {
            Department d = new Department();
            d.setCode(code);
            d.setName(name);
            d.setDescription(desc);
            departmentRepository.save(d);
            log.info("Created default Department: {}", code);
        }
    }

    private void createUserIfNotExists(String email, String fullName, String rawPassword, Set<Role> roles) {
        String normalized = email.trim().toLowerCase();

        User u;
        if (userRepository.existsByEmail(normalized)) {
            u = userRepository.findByEmail(normalized).orElseThrow();
        } else {
            u = new User();
            u.setEmail(normalized);
            u.setFullName(fullName);
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setStatus(UserStatus.ACTIVE);
            u.setRoles(new HashSet<>(roles));
            u = userRepository.save(u);
            log.info("Created user: {}", u.getEmail());
        }

        // If user has INTERN role, ensure InternProfile
        boolean isIntern = roles.stream().anyMatch(r -> "INTERN".equalsIgnoreCase(r.getCode()));
        if (isIntern) {
            if (internProfileRepository.findByUser_Id(u.getId()).isEmpty()) {
                InternProfile ip = new InternProfile();
                ip.setUser(u);
                internProfileRepository.save(ip);
                log.info("Auto-created InternProfile for user: {}", u.getEmail());
            }
        }

        // If user has MENTOR role, ensure Mentor profile
        boolean isMentor = roles.stream().anyMatch(r -> "MENTOR".equalsIgnoreCase(r.getCode()));
        if (isMentor) {
            if (mentorRepository.findByUser_Id(u.getId()).isEmpty()) {
                // Determine department (e.g. IT)
                Department itDept = departmentRepository.findByCode("IT").orElse(null);

                Mentor m = new Mentor();
                m.setUser(u);
                m.setDepartment(itDept);
                m.setTitle("Senior Mentor"); // Default title
                mentorRepository.save(m);
                log.info("Auto-created Mentor profile for user: {}", u.getEmail());
            }
        }
    }
}
