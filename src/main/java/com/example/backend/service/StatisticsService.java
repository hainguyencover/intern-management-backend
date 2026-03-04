package com.example.backend.service;

import com.example.backend.dto.InternCountStatDto;
import com.example.backend.dto.response.DashboardOverviewResponse;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.enums.GroupStatus;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final ProgramRepository programRepository;
    private final ProgramGroupRepository groupRepository;
    private final ApplicationRepository applicationRepository;
    private final InternDocumentRepository internDocumentRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.example.backend.repository.BackupJobRepository backupJobRepository;
    private final com.example.backend.repository.TaskRepository taskRepository;
    private final com.example.backend.repository.GroupMemberRepository groupMemberRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        return DashboardOverviewResponse.builder()
                .totalInterns(internProfileRepository.count())
                .totalMentors(mentorRepository.count())
                .totalPrograms(programRepository.count())
                .activeGroups(groupRepository.countByStatus(GroupStatus.ACTIVE))
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.SUBMITTED))
                .documentsToReview(internDocumentRepository.countByStatus("PENDING"))
                .activePrograms(programRepository.countByStatus(com.example.backend.enums.ProgramStatus.ACTIVE))
                .completionRate(calculateCompletionRate())
                .totalTasks(taskRepository.count())
                .completedTasks(taskRepository.countByStatus(com.example.backend.enums.TaskStatus.DONE))
                .systemHealth("GOOD") // Can be enhanced with real Actuator health check later
                .storageUsage(calculateStorageUsage())
                .recruitmentStats(DashboardOverviewResponse.RecruitmentStats.builder()
                        .applied(applicationRepository.countByStatus(ApplicationStatus.SUBMITTED))
                        .interviewing(applicationRepository.countByStatus(ApplicationStatus.APPROVED))
                        .offerSent(applicationRepository.countByStatus(ApplicationStatus.CONTRACT_SENT))
                        .onboarded(internProfileRepository.count())
                        .build())
                .recentActivities(getRecentActivities())
                .systemAlerts(getSystemAlerts())
                .upcomingBirthdays(getUpcomingBirthdays())
                .build();
    }

    private String calculateStorageUsage() {
        List<com.example.backend.entity.BackupJob> jobs = backupJobRepository.findTop30ByOrderByStartedAtDesc();
        Long backupSize = 0L;
        if (!jobs.isEmpty() && jobs.get(0).getFileSize() != null) {
            backupSize = jobs.get(0).getFileSize();
        }
        long mb = backupSize / (1024 * 1024);
        return mb + " MB / 5 GB (Cơ sở dữ liệu)";
    }

    private List<com.example.backend.dto.response.DashboardDtos.ActivityDto> getRecentActivities() {
        return auditLogRepository
                .findAll(org.springframework.data.domain.PageRequest.of(0, 5,
                        org.springframework.data.domain.Sort.by("createdAt").descending()))
                .stream().map(log -> com.example.backend.dto.response.DashboardDtos.ActivityDto.builder()
                        .content(log.getAction() + " on " + log.getEntityType())
                        .timeAgo(getTimeAgo(log.getCreatedAt()))
                        .color("blue")
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private List<com.example.backend.dto.response.DashboardDtos.AlertDto> getSystemAlerts() {
        List<com.example.backend.dto.response.DashboardDtos.AlertDto> alerts = new java.util.ArrayList<>();
        long pendingDocs = internDocumentRepository.countByStatus("PENDING");
        if (pendingDocs > 0) {
            alerts.add(com.example.backend.dto.response.DashboardDtos.AlertDto.builder()
                    .message("⚠️ Có " + pendingDocs + " tài liệu cần xét duyệt")
                    .type("warning").build());
        }
        long pendingApps = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED);
        if (pendingApps > 0) {
            alerts.add(com.example.backend.dto.response.DashboardDtos.AlertDto.builder()
                    .message("📝 Có " + pendingApps + " hồ sơ ứng tuyển mới")
                    .type("info").build());
        }
        return alerts;
    }

    private List<com.example.backend.dto.response.DashboardDtos.BirthdayDto> getUpcomingBirthdays() {
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        return internProfileRepository.findByBirthdayMonth(currentMonth).stream()
                .limit(5)
                .map(i -> com.example.backend.dto.response.DashboardDtos.BirthdayDto.builder()
                        .name(i.getUser().getFullName())
                        .position("Intern - " + i.getMajor()) // Simplified
                        .date(i.getDob() != null ? (i.getDob().getDayOfMonth() + "/" + i.getDob().getMonthValue())
                                : "N/A")
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private String getTimeAgo(java.time.LocalDateTime date) {
        if (date == null)
            return "";
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(date, java.time.LocalDateTime.now());
        if (minutes < 60)
            return minutes + " phút trước";
        long hours = minutes / 60;
        if (hours < 24)
            return hours + " giờ trước";
        return (hours / 24) + " ngày trước";
    }

    @Transactional(readOnly = true)
    public com.example.backend.dto.response.DashboardDtos.InternDashboardResponse getInternDashboard(Long userId) {
        com.example.backend.entity.InternProfile intern = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Intern profile not found for user: " + userId));
        Long internId = intern.getId();

        // Find active group
        java.util.Optional<com.example.backend.entity.GroupMember> membership = groupMemberRepository
                .findFirstByIntern_IdAndLeftAtIsNull(internId);
        Long groupId = membership.map(m -> m.getGroup().getId()).orElse(-1L);

        int completedTasks = (int) taskRepository
                .findByAssignee_Id(internId, org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(t -> t.getStatus() == com.example.backend.enums.TaskStatus.DONE).count();
        int allTasks = (int) taskRepository
                .findByAssignee_Id(internId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        java.time.LocalDate startDate = intern.getStartDate();
        java.time.LocalDate endDate = intern.getEndDate();

        // US: Prioritize Program dates if intern is in a group
        if (membership.isPresent()) {
            com.example.backend.entity.Program program = membership.get().getGroup().getProgram();
            if (program.getStartDate() != null) {
                startDate = program.getStartDate();
            }
            if (program.getEndDate() != null) {
                endDate = program.getEndDate();
            }
        }

        int daysInternship = 0;
        int totalDays = 90; // Default

        if (startDate != null) {
            long daysBetweenStartAndNow = java.time.temporal.ChronoUnit.DAYS.between(startDate,
                    java.time.LocalDate.now());
            daysInternship = (int) Math.max(0, daysBetweenStartAndNow); // Ensure non-negative

            if (endDate != null) {
                totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            }
        }

        String mentorName = intern.getMentor() != null ? intern.getMentor().getUser().getFullName() : "Chưa có";
        if ("Chưa có".equals(mentorName) && membership.isPresent()) {
            // Try to get mentor from group. Group has mentorId. We need to fetch Mentor
            // entity.
            Long mentorId = membership.get().getGroup().getMentorId();
            if (mentorId != null) {
                mentorName = mentorRepository.findById(mentorId)
                        .map(m -> m.getUser().getFullName())
                        .orElse("Chưa có");
            }
        }

        return com.example.backend.dto.response.DashboardDtos.InternDashboardResponse.builder()
                .internName(intern.getUser().getFullName())
                .position("Thực tập sinh " + (intern.getMajor() != null ? intern.getMajor() : ""))
                .mentorName(mentorName)
                .tasksCompleted(completedTasks)
                .tasksTotal(allTasks)
                .daysInternship(Math.max(0, daysInternship))
                .totalDays(totalDays)
                .recentActivities(java.util.Collections.emptyList()) // Todo: Query audit log
                .build();
    }

    private Double calculateCompletionRate() {
        long totalInterns = internProfileRepository.count();
        if (totalInterns == 0)
            return 0.0;

        long completedInterns = internProfileRepository.findAll().stream()
                .filter(i -> i.getEndDate() != null && i.getEndDate().isBefore(java.time.LocalDate.now()))
                .count();

        return ((double) completedInterns / totalInterns) * 100.0;
    }

    @Transactional(readOnly = true)
    public List<InternCountStatDto> getUniversityStats() {
        return internProfileRepository.countByUniversityAndMajor();
    }
}
