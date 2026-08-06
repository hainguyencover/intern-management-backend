package com.holaho.intern.controller;

import com.holaho.intern.entity.Department;
import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.Application;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.enums.ReviewDecision;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.shared.dto.request.ReviewApplicationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class InternLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InternProfileRepository internProfileRepository;

    @Autowired
    private MentorRepository mentorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private User candidateUser;
    private InternProfile candidateProfile;
    private Program program;
    private User hrUser;
    private User mentorUser;
    private Mentor mentorProfile;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByCode("INTERN").isEmpty()) {
            Role r = new Role();
            r.setCode("INTERN");
            r.setName("Intern");
            roleRepository.save(r);
        }
        if (roleRepository.findByCode("HR").isEmpty()) {
            Role r = new Role();
            r.setCode("HR");
            r.setName("HR");
            roleRepository.save(r);
        }
        if (roleRepository.findByCode("MENTOR").isEmpty()) {
            Role r = new Role();
            r.setCode("MENTOR");
            r.setName("Mentor");
            roleRepository.save(r);
        }

        // Candidate User & Profile
        candidateUser = new User();
        candidateUser.setEmail("lifecycle-candidate@example.com");
        candidateUser.setFullName("Lifecycle Candidate");
        candidateUser.setPasswordHash("hash");
        candidateUser.setStatus(UserStatus.ACTIVE);
        candidateUser = userRepository.save(candidateUser);

        candidateProfile = new InternProfile();
        candidateProfile.setUser(candidateUser);
        candidateProfile.setUniversity("Hust");
        candidateProfile.setMajor("CS");
        candidateProfile = internProfileRepository.save(candidateProfile);

        // HR User
        hrUser = new User();
        hrUser.setEmail("lifecycle-hr@example.com");
        hrUser.setFullName("HR Manager");
        hrUser.setPasswordHash("hash");
        hrUser.setStatus(UserStatus.ACTIVE);
        hrUser = userRepository.save(hrUser);

        // Mentor User & Profile
        mentorUser = new User();
        mentorUser.setEmail("lifecycle-mentor@example.com");
        mentorUser.setFullName("Mentor Instructor");
        mentorUser.setPasswordHash("hash");
        mentorUser.setStatus(UserStatus.ACTIVE);
        mentorUser = userRepository.save(mentorUser);

        mentorProfile = new Mentor();
        mentorProfile.setUser(mentorUser);
        mentorProfile.setTitle("Senior Mentor");
        mentorProfile = mentorRepository.save(mentorProfile);

        // Dept & Program
        Department dept = new Department();
        dept.setCode("DEV_DEPT");
        dept.setName("Development");
        dept = departmentRepository.save(dept);

        program = new Program();
        program.setCode("JAVA_INTERN_2026");
        program.setName("Java Internship 2026");
        program.setDepartment(dept);
        program.setStatus(ProgramStatus.ACTIVE);
        program = programRepository.save(program);
    }

    @Test
    void lifecycle_AutoCreateAndManualTransitions() throws Exception {
        // 1. Setup accepted application
        Application app = new Application();
        app.setIntern(candidateProfile);
        app.setProgram(program);
        app.setPosition("Backend Dev");
        app.setStatus(ApplicationStatus.SUBMITTED);
        app = applicationRepository.save(app);

        // Review & Approve -> Auto transitions profile status to ONBOARDING and assigns ROLE_INTERN
        ReviewApplicationRequest reviewReq = new ReviewApplicationRequest(ReviewDecision.APPROVE, "Welcome!");
        mockMvc.perform(postWithTenant("/api/v1/applications/" + app.getId() + "/review", reviewReq)
                        .principal(() -> hrUser.getEmail()))
                .andExpect(status().isOk());

        // Verify profile status changed to ONBOARDING
        InternProfile profile = internProfileRepository.findById(candidateProfile.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ONBOARDING", profile.getStatus());

        // Verify ROLE_INTERN was assigned
        User user = userRepository.findById(candidateUser.getId()).orElseThrow();
        boolean hasInternRole = user.getRoles().stream().anyMatch(r -> "INTERN".equals(r.getCode()));
        org.junit.jupiter.api.Assertions.assertTrue(hasInternRole);

        // 2. HR transitions profile status to ACTIVE
        mockMvc.perform(putWithTenant("/api/v1/interns/profiles/" + profile.getId() + "/status?status=ACTIVE"))
                .andExpect(status().isOk());

        profile = internProfileRepository.findById(profile.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", profile.getStatus());

        // 3. HR assigns Mentor to Intern
        mockMvc.perform(putWithTenant("/api/v1/interns/profiles/" + profile.getId() + "/assign-mentor?mentorUserId=" + mentorUser.getId()))
                .andExpect(status().isOk());

        profile = internProfileRepository.findById(profile.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(mentorProfile.getId(), profile.getMentor().getId());
    }
}
