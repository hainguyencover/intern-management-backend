package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.request.ApplicationSubmitRequest;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ApplicationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InternProfileRepository internProfileRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String internToken;
    private User internUser;
    private Long programId;

    @BeforeEach
    void setUp() {
        Role internRole = roleRepository.findByCode("INTERN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode("INTERN");
                    role.setName("Intern Role");
                    return roleRepository.save(role);
                });

        internUser = new User();
        internUser.setEmail("intern_test@example.com");
        internUser.setPasswordHash(passwordEncoder.encode("password123"));
        internUser.setFullName("Intern Test");
        internUser.setStatus(UserStatus.ACTIVE);
        internUser.setRoles(Collections.singleton(internRole));
        internUser = userRepository.save(internUser);

        InternProfile profile = new InternProfile();
        profile.setUser(internUser);
        profile.setStudentCode("ST_TEST_01");
        internProfileRepository.save(profile);

        com.holaho.intern.entity.Department department = new com.holaho.intern.entity.Department();
        department.setName("Test Department");
        department.setCode("DEPT_TEST");
        department = departmentRepository.save(department);

        com.holaho.intern.entity.Program program = new com.holaho.intern.entity.Program();
        program.setName("Test Program");
        program.setDepartment(department);
        program.setStartDate(java.time.LocalDate.now());
        program.setEndDate(java.time.LocalDate.now().plusMonths(3));
        program.setStatus(com.holaho.intern.shared.enums.ProgramStatus.ACTIVE);
        program = programRepository.save(program);
        programId = program.getId();

        // Generate token for the intern user
        com.holaho.intern.shared.security.CustomUserDetails userDetails = new com.holaho.intern.shared.security.CustomUserDetails(
                internUser);
        internToken = jwtTokenProvider.generateToken(userDetails);
    }

    @Test
    void submitApplication_Success() throws Exception {
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.setPosition("Java Developer Intern");
        request.setProgramId(programId);
        request.setNote("I love coding!");

        mockMvc.perform(postWithTenant("/api/v1/applications", request)
                .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.position").value("Java Developer Intern"));
    }

    @Test
    void submitApplication_Unauthorized() throws Exception {
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.setPosition("Java Developer Intern");

        mockMvc.perform(postWithTenant("/api/v1/applications", request))
                .andExpect(status().isUnauthorized());
    }
}
