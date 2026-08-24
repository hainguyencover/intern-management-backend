package com.holaho.intern.shared.config;

import com.holaho.intern.entity.Application;
import com.holaho.intern.entity.Department;
import com.holaho.intern.entity.Evaluation;
import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.WeeklyReport;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.repository.WeeklyReportRepository;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.enums.WeeklyReportStatus;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Deterministic Seed Generator for Phase 2 Performance Benchmarking
 * Uses fixed seed Random(20260807L) to ensure 100% reproducible dataset.
 */
@Component
@Profile("benchmark-seed")
@RequiredArgsConstructor
@Slf4j
public class BenchmarkDataSeeder implements CommandLineRunner {

    private static final long SEED = 20260807L;
    private final Random random = new Random(SEED);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final ApplicationRepository applicationRepository;
    private final WeeklyReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final EvaluationRepository evaluationRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String[] UNIVERSITIES = {
            "FPT University", "HUST", "PTIT", "VNU", "UIT", "DUT"
    };

    private static final String[] MAJORS = {
            "Software Engineering", "Computer Science", "AI", "Information Systems", "Cyber Security", "Data Science"
    };

    private static final Double[] GPAS = { 2.5, 3.0, 3.2, 3.4, 3.6, 3.8 };

    private static final String[] COMPLETED_TASKS = {
            "Implemented JWT Authentication & Refresh Tokens",
            "Optimized Redis Cache Layer & Connection Pool",
            "Completed Spring Boot Migration & ArchUnit Rules",
            "Resolved PR Code Review Comments on Multi-tenancy",
            "Configured Prometheus Micrometer Metrics & Actuator",
            "Refactored Database Queries for N+1 Elimination"
    };

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=================================================");
        log.info("Starting Deterministic Benchmark Data Seeding...");
        log.info("Random Seed: {}", SEED);
        log.info("=================================================");

        Role internRole = roleRepository.findByCode("INTERN").orElseGet(() -> {
            Role r = new Role();
            r.setCode("INTERN");
            r.setName("Intern");
            return roleRepository.save(r);
        });

        Role mentorRole = roleRepository.findByCode("MENTOR").orElseGet(() -> {
            Role r = new Role();
            r.setCode("MENTOR");
            r.setName("Mentor");
            return roleRepository.save(r);
        });

        Department dept = departmentRepository.findByCode("IT").orElseGet(() -> {
            Department d = new Department();
            d.setCode("IT");
            d.setName("Information Technology");
            return departmentRepository.save(d);
        });

        Program program = programRepository.findAll().stream().findFirst().orElseGet(() -> {
            Program p = new Program();
            p.setName("Enterprise Internship 2026");
            p.setDepartment(dept);
            p.setStartDate(LocalDate.now().minusMonths(6));
            p.setEndDate(LocalDate.now().plusMonths(6));
            p.setStatus(ProgramStatus.ACTIVE);
            return programRepository.save(p);
        });

        // 1. Seed Mentors
        List<Mentor> mentors = new ArrayList<>();
        String encodedPass = passwordEncoder.encode("benchmark123");

        for (int i = 1; i <= 50; i++) {
            String email = "mentor" + i + "@benchmark.com";
            if (!userRepository.existsByEmail(email)) {
                User u = new User();
                u.setEmail(email);
                u.setFullName("Mentor " + i);
                u.setPasswordHash(encodedPass);
                u.setStatus(UserStatus.ACTIVE);
                u.setRoles(Set.of(mentorRole));
                u = userRepository.save(u);

                Mentor m = new Mentor();
                m.setUser(u);
                m.setDepartment(dept);
                m.setTitle("Senior Tech Lead");
                mentors.add(mentorRepository.save(m));
            }
        }

        // 2. Seed 1,000 Interns (Baseline Batch)
        List<InternProfile> interns = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            String email = "intern" + i + "@benchmark.com";
            if (!userRepository.existsByEmail(email)) {
                User u = new User();
                u.setEmail(email);
                u.setFullName("Intern User " + i);
                u.setPasswordHash(encodedPass);
                u.setStatus(UserStatus.ACTIVE);
                u.setRoles(Set.of(internRole));
                u = userRepository.save(u);

                InternProfile ip = new InternProfile();
                ip.setUser(u);
                ip.setStudentCode("STU" + (100000 + i));
                ip.setUniversity(UNIVERSITIES[random.nextInt(UNIVERSITIES.length)]);
                ip.setMajor(MAJORS[random.nextInt(MAJORS.length)]);
                ip.setGpa(GPAS[random.nextInt(GPAS.length)]);
                ip.setStartDate(LocalDate.now().minusMonths(3));
                ip.setEndDate(LocalDate.now().plusMonths(3));
                if (!mentors.isEmpty()) {
                    ip.setMentor(mentors.get(random.nextInt(mentors.size())));
                }
                interns.add(internProfileRepository.save(ip));
            }
        }

        log.info("Seeded Mentors & Intern Profiles. Seeding Weekly Reports & Tasks...");

        // 3. Seed Weekly Reports & Tasks with Realistic Text
        int reportCount = 0;
        int taskCount = 0;

        for (InternProfile intern : interns) {
            // Seed 5 Weekly Reports per intern
            for (int w = 1; w <= 5; w++) {
                WeeklyReport report = new WeeklyReport();
                report.setIntern(intern);
                report.setWeekNumber(w);
                report.setTitle("Weekly Report - Week " + w);
                report.setWeekStart(LocalDate.now().minusWeeks(6 - w));
                report.setWeekEnd(LocalDate.now().minusWeeks(5 - w));
                report.setReportDate(LocalDate.now().minusWeeks(5 - w));
                report.setCompletedWork(COMPLETED_TASKS[random.nextInt(COMPLETED_TASKS.length)]);
                report.setPlannedWork("Next week: Continue benchmark execution & SLA validation");
                report.setChallenges("None. Code quality checks and ArchUnit rules passed.");
                report.setLearnings("Learned PostgreSQL/MySQL EXPLAIN ANALYZE execution plan reading.");
                report.setStatus(WeeklyReportStatus.REVIEWED);
                report.setRating(4 + random.nextInt(2));
                reportRepository.save(report);
                reportCount++;
            }

            // Seed Evaluation
            Evaluation eval = new Evaluation();
            eval.setIntern(intern);
            eval.setMentor(intern.getMentor());
            eval.setPeriod("FINAL");
            eval.setScore(75 + random.nextInt(25)); // 75 - 100
            eval.setComment("Strong technical skills and clean code practices.");
            evaluationRepository.save(eval);
        }

        log.info("=================================================");
        log.info("Benchmark Data Seeding Complete!");
        log.info("Seeded Interns: {}", interns.size());
        log.info("Seeded Reports: {}", reportCount);
        log.info("=================================================");
    }
}
