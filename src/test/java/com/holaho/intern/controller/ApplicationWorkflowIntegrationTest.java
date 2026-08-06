package com.holaho.intern.controller;

import com.holaho.intern.entity.Department;
import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.Application;
import com.holaho.intern.entity.Interview;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.enums.ReviewDecision;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.repository.InterviewRepository;
import com.holaho.intern.shared.dto.request.ApplicationSubmitRequest;
import com.holaho.intern.shared.dto.request.ReviewApplicationRequest;
import com.holaho.intern.shared.dto.request.InterviewScheduleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ApplicationWorkflowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InternProfileRepository internProfileRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    private User candidateUser;
    private InternProfile candidateProfile;
    private Program program;
    private User hrUser;

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

        // Candidate User & Profile
        candidateUser = new User();
        candidateUser.setEmail("candidate@example.com");
        candidateUser.setFullName("Candidate User");
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
        hrUser.setEmail("hr@example.com");
        hrUser.setFullName("HR Manager");
        hrUser.setPasswordHash("hash");
        hrUser.setStatus(UserStatus.ACTIVE);
        hrUser = userRepository.save(hrUser);

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
    void workflow_Submit_Screen_ScheduleInterview_Review_Success() throws Exception {
        // 1. Submit Application
        ApplicationSubmitRequest submitReq = new ApplicationSubmitRequest();
        submitReq.setProgramId(program.getId());
        submitReq.setPosition("Backend Dev");
        submitReq.setNote("Keen to learn");

        mockMvc.perform(postWithTenant("/api/v1/applications", submitReq)
                        .principal(() -> candidateUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        Application app = applicationRepository.findByIntern_Id(candidateProfile.getId()).get(0);

        // 2. Schedule Interview (Transitions to INTERVIEWING)
        InterviewScheduleRequest scheduleReq = new InterviewScheduleRequest();
        scheduleReq.setApplicationId(app.getId());
        scheduleReq.setScheduledTime(LocalDateTime.now().plusDays(2));
        scheduleReq.setLocation("Zoom Link");
        scheduleReq.setInterviewerId(hrUser.getId());

        mockMvc.perform(postWithTenant("/api/v1/interviews", scheduleReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        // Verify Application Status changed to INTERVIEWING
        mockMvc.perform(getWithTenant("/api/v1/applications/" + app.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERVIEWING"));

        // 3. Review & Approve (Transitions to APPROVED)
        ReviewApplicationRequest reviewReq = new ReviewApplicationRequest(ReviewDecision.APPROVE, "Looks great!");
        mockMvc.perform(postWithTenant("/api/v1/applications/" + app.getId() + "/review", reviewReq)
                        .principal(() -> hrUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }
}
