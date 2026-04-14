package com.holaho.intern.service;

import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.repository.RoleRepository;


import com.holaho.intern.shared.dto.hrm.HrmUserDto;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HrmService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Simulate external API call
    public List<HrmUserDto> fetchEmployeesFromHrm() {
        // In a real app, this would use RestTemplate/WebClient to call external HRM API
        List<HrmUserDto> mockData = new ArrayList<>();
        mockData.add(new HrmUserDto("HRM001", "NguyÃ¡Â»â€¦n VÃ„Æ’n A", "nguyen.a@example.com", "Engineering", "Senior Engineer",
                "ACTIVE"));
        mockData.add(new HrmUserDto("HRM002", "TrÃ¡ÂºÂ§n ThÃ¡Â»â€¹ B", "tran.b@example.com", "HR", "Recruiter", "ACTIVE"));
        mockData.add(new HrmUserDto("HRM003", "LÃƒÂª VÃ„Æ’n C", "le.c@example.com", "Engineering", "Team Lead", "ACTIVE"));
        // Add a new one that likely doesn't exist
        mockData.add(
                new HrmUserDto("HRM004", "PhÃ¡ÂºÂ¡m VÃ„Æ’n MÃ¡Â»â€ºi", "pham.moi@example.com", "Mentorship", "Mentor", "ACTIVE"));
        return mockData;
    }

    @Transactional
    public String syncData() {
        List<HrmUserDto> hrmUsers = fetchEmployeesFromHrm();
        int created = 0;
        int updated = 0;

        for (HrmUserDto hrmUser : hrmUsers) {
            try {
                if (!"ACTIVE".equalsIgnoreCase(hrmUser.getStatus())) {
                    continue; // Skip inactive
                }

                User user = userRepository.findByEmail(hrmUser.getEmail()).orElse(null);
                if (user == null) {
                    // Create new user
                    user = new User();
                    user.setEmail(hrmUser.getEmail());
                    user.setFullName(hrmUser.getFullName());
                    user.setPasswordHash(passwordEncoder.encode("123456")); // Default password

                    // Basic role logic
                    String roleCode = "HR".equalsIgnoreCase(hrmUser.getDepartment()) ? "HR" : "MENTOR";
                    com.holaho.intern.user.entity.Role role = roleRepository.findByCode(roleCode)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

                    user.setRoles(java.util.Collections.singleton(role));

                    userRepository.save(user);
                    created++;
                } else {
                    // Update existing info
                    user.setFullName(hrmUser.getFullName());
                    userRepository.save(user);
                    updated++;
                }
            } catch (Exception e) {
                log.error("Error syncing user: " + hrmUser.getEmail(), e);
            }
        }
        return String.format("Ã„ÂÃƒÂ£ Ã„â€˜Ã¡Â»â€œng bÃ¡Â»â„¢ thÃƒÂ nh cÃƒÂ´ng. ThÃƒÂªm mÃ¡Â»â€ºi: %d, CÃ¡ÂºÂ­p nhÃ¡ÂºÂ­t: %d", created, updated);
    }
}

