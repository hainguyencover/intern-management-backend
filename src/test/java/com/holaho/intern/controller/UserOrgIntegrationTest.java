package com.holaho.intern.controller;

import com.holaho.intern.entity.Department;
import com.holaho.intern.entity.Program;
import com.holaho.intern.shared.dto.request.*;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.entity.Permission;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.entity.UserPermission;
import com.holaho.intern.user.repository.PermissionRepository;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class UserOrgIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByCode("ADMIN").isEmpty()) {
            Role adminRole = new Role();
            adminRole.setCode("ADMIN");
            adminRole.setName("Admin Role");
            roleRepository.save(adminRole);
        }
        if (roleRepository.findByCode("INTERN").isEmpty()) {
            Role internRole = new Role();
            internRole.setCode("INTERN");
            internRole.setName("Intern Role");
            roleRepository.save(internRole);
        }
    }

    @Test
    void userCrud_SuccessFlow() throws Exception {
        // 1. Create User
        CreateUserRequest createReq = new CreateUserRequest();
        createReq.setEmail("crud@example.com");
        createReq.setFullName("CRUD User");
        createReq.setPhone("0987654321");
        createReq.setPassword("Password123!");
        createReq.setRoleCodes(List.of("INTERN"));

        mockMvc.perform(postWithTenant("/api/v1/admin/users", createReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("crud@example.com"));

        User user = userRepository.findByEmail("crud@example.com").orElseThrow();

        // 2. Update User details
        UpdateUserRequest updateReq = new UpdateUserRequest();
        updateReq.setFullName("Updated CRUD Name");
        updateReq.setPhone("0123456789");

        mockMvc.perform(putWithTenant("/api/v1/admin/users/" + user.getId(), updateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated CRUD Name"));

        // 3. Delete User
        mockMvc.perform(deleteWithTenant("/api/v1/admin/users/" + user.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void userPermissions_OverrideFlow() throws Exception {
        User user = new User();
        user.setEmail("perm-override@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setFullName("Override User");
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Permission p = new Permission();
        p.setCode("TEST_PERM");
        p.setName("Test Permission");
        p.setModule("TEST");
        p = permissionRepository.save(p);

        UpdateUserPermissionsRequest overrideReq = UpdateUserPermissionsRequest.builder()
                .overrides(List.of(
                        UpdateUserPermissionsRequest.PermissionOverrideDto.builder()
                                .permissionId(p.getId())
                                .mode(UserPermission.PermissionMode.GRANT)
                                .build()
                ))
                .build();

        mockMvc.perform(putWithTenant("/api/v1/admin/users/" + user.getId() + "/permissions", overrideReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overrides[0].permissionCode").value("TEST_PERM"))
                .andExpect(jsonPath("$.data.overrides[0].mode").value("GRANT"));
    }

    @Test
    void departmentAndProgram_CrudFlow() throws Exception {
        // 1. Create Department
        DepartmentRequest deptReq = new DepartmentRequest();
        deptReq.setCode("DEPT_TEST");
        deptReq.setName("Test Dept");
        deptReq.setDescription("Description");

        mockMvc.perform(postWithTenant("/api/v1/departments", deptReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DEPT_TEST"));

        Department dept = departmentRepository.findByCode("DEPT_TEST").orElseThrow();

        // 2. Create Program
        CreateProgramRequest progReq = new CreateProgramRequest();
        progReq.setCode("PROG_TEST");
        progReq.setName("Test Program");
        progReq.setDepartmentId(dept.getId());
        progReq.setStartDate(java.time.LocalDate.now());
        progReq.setEndDate(java.time.LocalDate.now().plusMonths(3));

        mockMvc.perform(postWithTenant("/api/v1/programs", progReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("PROG_TEST"));
    }
}
