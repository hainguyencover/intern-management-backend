package com.holaho.intern.controller;

import com.holaho.intern.entity.Department;
import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.entity.NotificationPreference;
import com.holaho.intern.notification.repository.NotificationPreferenceRepository;
import com.holaho.intern.notification.repository.NotificationRepository;
import com.holaho.intern.notification.dto.NotificationPreferenceDto;
import com.holaho.intern.notification.entity.EmailQueue;
import com.holaho.intern.notification.repository.EmailQueueRepository;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.shared.dto.request.TaskRequest;
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
class NotificationIntegrationTest extends BaseIntegrationTest {

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
    private ProgramGroupRepository groupRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private EmailQueueRepository emailQueueRepository;

    private User internUser;
    private InternProfile internProfile;
    private User mentorUser;
    private Mentor mentorProfile;
    private ProgramGroup group;

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
        internUser.setEmail("notif-intern@example.com");
        internUser.setFullName("Notif Intern");
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
        mentorUser.setEmail("notif-mentor@example.com");
        mentorUser.setFullName("Notif Mentor");
        mentorUser.setPasswordHash("hash");
        mentorUser.setStatus(UserStatus.ACTIVE);
        mentorUser = userRepository.save(mentorUser);

        mentorProfile = new Mentor();
        mentorProfile.setUser(mentorUser);
        mentorProfile.setTitle("Senior Mentor");
        mentorProfile = mentorRepository.save(mentorProfile);

        // Dept, Program & Group
        Department dept = new Department();
        dept.setCode("DEV_DEPT");
        dept.setName("Development");
        dept = departmentRepository.save(dept);

        Program program = new Program();
        program.setCode("PROG_2026");
        program.setName("Internship 2026");
        program.setDepartment(dept);
        program.setStatus(ProgramStatus.ACTIVE);
        program = programRepository.save(program);

        group = new ProgramGroup();
        group.setName("Backend Group 1");
        group.setProgram(program);
        group.setMentorId(mentorUser.getId());
        group = groupRepository.save(group);
    }

    @Test
    void workflow_NotificationPreferences_And_EventDispatches() throws Exception {
        // 1. Update Notification Preferences via PUT /api/v1/notifications/preferences
        NotificationPreferenceDto.Request prefReq = new NotificationPreferenceDto.Request(
                "TaskAssignedEvent",
                true,  // email
                false, // websocket
                true   // in_app
        );

        mockMvc.perform(putWithTenant("/api/v1/notifications/preferences", prefReq)
                        .principal(() -> internUser.getEmail()))
                .andExpect(status().isOk());

        // Verify preference saved in DB
        List<NotificationPreference> prefs = preferenceRepository.findByUser_Id(internUser.getId());
        org.junit.jupiter.api.Assertions.assertFalse(prefs.isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(prefs.get(0).isEmailEnabled());
        org.junit.jupiter.api.Assertions.assertFalse(prefs.get(0).isWebsocketEnabled());

        // 2. Mentor creates a task -> triggers TaskAssignedEvent
        TaskRequest taskReq = new TaskRequest();
        taskReq.setGroupId(group.getId());
        taskReq.setTitle("Event Testing Task");
        taskReq.setDescription("Testing the domain event system");
        taskReq.setAssigneeId(internProfile.getId());
        taskReq.setDueDate(LocalDateTime.now().plusDays(2));

        mockMvc.perform(postWithTenant("/api/v1/tasks", taskReq)
                        .principal(() -> mentorUser.getEmail()))
                .andExpect(status().isOk());

        // 3. Verify in-app notification was created
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                internUser.getId(),
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();
        org.junit.jupiter.api.Assertions.assertFalse(notifications.isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(
                notifications.get(0).getTitle().contains("Công việc mới được giao")
        );

        // 4. Verify email was queued in the Email Queue
        List<EmailQueue> queuedEmails = emailQueueRepository.findByStatus("PENDING");
        org.junit.jupiter.api.Assertions.assertFalse(queuedEmails.isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(internUser.getEmail(), queuedEmails.get(0).getRecipient());

        // 5. Query user notifications via GET /api/v1/notifications (pagination metadata format check)
        mockMvc.perform(getWithTenant("/api/v1/notifications")
                        .principal(() -> internUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.totalElements").exists());
    }
}
