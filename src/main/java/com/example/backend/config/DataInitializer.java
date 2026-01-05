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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InternProfileRepository internProfileRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting Data Initialization...");

        Role adminRole = createRoleIfNotExists("ADMIN", "Administrator");
        Role hrRole = createRoleIfNotExists("HR", "Human Resources");
        Role mentorRole = createRoleIfNotExists("MENTOR", "Mentor");
        Role internRole = createRoleIfNotExists("INTERN", "Intern");

        createUserIfNotExists("admin@company.com", "System Admin", "admin123", Set.of(adminRole));
        createUserIfNotExists("hr@company.com", "HR Manager", "hr123", Set.of(hrRole));
        createUserIfNotExists("intern@student.com", "Intern Demo", "intern123", Set.of(internRole));

        log.info("Data Initialization Complete!");
        log.info("Test credentials:");
        log.info("  Admin: admin@company.com / admin123");
        log.info("  HR:    hr@company.com / hr123");
        log.info("  Intern:intern@student.com / intern123");
    }

    private Role createRoleIfNotExists(String code, String name) {
        // store role codes without ROLE_ prefix (e.g. ADMIN, HR, INTERN)
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setCode(code);
                    r.setName(name);
                    return roleRepository.save(r);
                });
    }

    private void createUserIfNotExists(String email, String fullName, String rawPassword, Set<Role> roles) {
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized))
            return;

        User u = new User();
        u.setEmail(normalized);
        u.setFullName(fullName);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setStatus(UserStatus.ACTIVE);
        u.setRoles(new HashSet<>(roles));

        u = userRepository.save(u);

        // If user has INTERN role, ensure an InternProfile exists for them to avoid 404
        // later
        boolean isIntern = roles.stream().anyMatch(r -> "INTERN".equalsIgnoreCase(r.getCode()));
        if (isIntern) {
            boolean existsProfile = internProfileRepository.findByUser_Id(u.getId()).isPresent();
            if (!existsProfile) {
                InternProfile ip = new InternProfile();
                ip.setUser(u);
                // other fields left null/empty; will be filled later
                internProfileRepository.save(ip);
                log.info("Auto-created InternProfile for user: {} (id={})", u.getEmail(), u.getId());
            }
        }
    }
}
