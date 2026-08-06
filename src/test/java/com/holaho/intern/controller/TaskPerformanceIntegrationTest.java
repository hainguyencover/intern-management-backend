package com.holaho.intern.controller;

import com.holaho.intern.entity.Department;
import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.Evaluation;
import com.holaho.intern.entity.WeeklyReport;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.repository.WeeklyReportRepository;
import com.holaho.intern.shared.dto.request.TaskRequest;
import com.holaho.intern.shared.dto.request.TaskUpdateRequest;
import com.holaho.intern.shared.dto.request.WeeklyReportRequest;
import com.holaho.intern.shared.dto.request.ReviewWeeklyReportRequest;
import com.holaho.intern.shared.dto.request.EvaluationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class TaskPerformanceIntegrationTest extends BaseIntegrationTest {

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
    private EvaluationRepository evaluationRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    private User internUser;
    private InternProfile internProfile;
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
        if (roleRepository.findByCode("MENTOR").isEmpty()) {
            Role r = new Role();
            r.setCode("MENTOR");
            r.setName("Mentor");
            roleRepository.save(r);
        }

        // Intern
        internUser = new User();
        internUser.setEmail("intern-perf@example.com");
        internUser.setFullName("Intern Performance");
        internUser.setPasswordHash("hash");
        internUser.setStatus(UserStatus.ACTIVE);
        internUser = userRepository.save(internUser);

        internProfile = new InternProfile();
        internProfile.setUser(internUser);
        internProfile.setUniversity("Hust");
        internProfile.setMajor("CS");
        internProfile = internProfileRepository.save(internProfile);

        // Mentor
        mentorUser = new User();
        mentorUser.setEmail("mentor-perf@example.com");
        mentorUser.setFullName("Mentor Performance");
        mentorUser.setPasswordHash("hash");
        mentorUser.setStatus(UserStatus.ACTIVE);
        mentorUser = userRepository.save(mentorUser);

        mentorProfile = new Mentor();
        mentorProfile.setUser(mentorUser);
        mentorProfile.setTitle("Senior Dev");
        mentorProfile = mentorRepository.save(mentorProfile);
    }

    @Test
    void workflow_Task_WeeklyReport_Evaluation() throws Exception {
        // 1. Create Task (Mentor)
        TaskRequest taskReq = new TaskRequest();
        taskReq.setTitle("Perform DB Tuning");
        taskReq.setDescription("Tune SQL queries");
        taskReq.setAssigneeId(internProfile.getId());
        taskReq.setDueDate(LocalDateTime.now().plusDays(5));

        mockMvc.perform(postWithTenant("/api/v1/tasks", taskReq)
                        .principal(() -> mentorUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Perform DB Tuning"));

        // 2. Submit Weekly Report (Intern)
        WeeklyReportRequest reportReq = new WeeklyReportRequest();
        reportReq.setWeekStartDate(LocalDate.now().minusDays(5));
        reportReq.setWeekEndDate(LocalDate.now());
        reportReq.setTasksCompleted("Tuned 5 queries");
        reportReq.setTasksPlanned("Next tasks");
        reportReq.setIssuesFaced("No blockers");
        reportReq.setHoursWorked(40);

        mockMvc.perform(postWithTenant("/api/v1/reports/weekly", reportReq)
                        .principal(() -> internUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tasksCompleted").value("Tuned 5 queries"));

        WeeklyReport report = weeklyReportRepository.findAll().get(0);

        // 3. Review Weekly Report (Mentor)
        ReviewWeeklyReportRequest reviewReq = new ReviewWeeklyReportRequest("EXCELLENT", "Great work!");
        mockMvc.perform(putWithTenant("/api/v1/reports/" + report.getId() + "/feedback", reviewReq)
                        .principal(() -> mentorUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mentorAssessment").value("EXCELLENT"));

        // 4. Create Performance Evaluation (Mentor)
        EvaluationRequest evalReq = new EvaluationRequest();
        evalReq.setInternId(internProfile.getId());
        evalReq.setPeriod("MID_TERM");
        evalReq.setScore(9);
        evalReq.setComment("Highly dedicated and smart");

        mockMvc.perform(postWithTenant("/api/v1/evaluations", evalReq)
                        .principal(() -> mentorUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(9));
    }
}
