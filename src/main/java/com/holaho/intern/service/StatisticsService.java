package com.holaho.intern.service;

import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.repository.AuditLogRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.dto.response.DashboardOverviewResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.GroupStatus;

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
    private final com.holaho.intern.repository.BackupJobRepository backupJobRepository;
    private final com.holaho.intern.task.repository.TaskRepository taskRepository;
    private final com.holaho.intern.repository.GroupMemberRepository groupMemberRepository;

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "dashboardOverview", keyGenerator = "tenantAwareKeyGenerator")
    public DashboardOverviewResponse getOverview() {
        return DashboardOverviewResponse.builder()
                .totalInterns(internProfileRepository.count())
                .totalMentors(mentorRepository.count())
                .totalPrograms(programRepository.count())
                .activeGroups(groupRepository.countByStatus(GroupStatus.ACTIVE))
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.SUBMITTED))
                .documentsToReview(internDocumentRepository.countByStatus("PENDING"))
                .activePrograms(programRepository.countByStatus(com.holaho.intern.shared.enums.ProgramStatus.ACTIVE))
                .completionRate(calculateCompletionRate())
                .totalTasks(taskRepository.count())
                .completedTasks(taskRepository.countByStatus(com.holaho.intern.shared.enums.TaskStatus.DONE))
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
        List<com.holaho.intern.entity.BackupJob> jobs = backupJobRepository.findTop30ByOrderByStartedAtDesc();
        Long backupSize = 0L;
        if (!jobs.isEmpty() && jobs.get(0).getFileSize() != null) {
            backupSize = jobs.get(0).getFileSize();
        }
        long mb = backupSize / (1024 * 1024);
        return mb + " MB / 5 GB (Cơ sở dữ liệu)";
    }

    private List<com.holaho.intern.shared.dto.response.DashboardDtos.ActivityDto> getRecentActivities() {
        return auditLogRepository
                .findAll(org.springframework.data.domain.PageRequest.of(0, 5,
                        org.springframework.data.domain.Sort.by("createdAt").descending()))
                .stream().map(log -> com.holaho.intern.shared.dto.response.DashboardDtos.ActivityDto.builder()
                        .content(log.getAction() + " on " + log.getEntityType())
                        .timeAgo(getTimeAgo(log.getCreatedAt()))
                        .color("blue")
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private List<com.holaho.intern.shared.dto.response.DashboardDtos.AlertDto> getSystemAlerts() {
        List<com.holaho.intern.shared.dto.response.DashboardDtos.AlertDto> alerts = new java.util.ArrayList<>();
        long pendingDocs = internDocumentRepository.countByStatus("PENDING");
        if (pendingDocs > 0) {
            alerts.add(com.holaho.intern.shared.dto.response.DashboardDtos.AlertDto.builder()
                    .message("⚠ Có " + pendingDocs + " tài liệu cần xét duyệt")
                    .type("warning").build());
        }
        long pendingApps = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED);
        if (pendingApps > 0) {
            alerts.add(com.holaho.intern.shared.dto.response.DashboardDtos.AlertDto.builder()
                    .message("ℹ Có " + pendingApps + " hồ sơ ứng tuyển mới")
                    .type("info").build());
        }
        return alerts;
    }

    private List<com.holaho.intern.shared.dto.response.DashboardDtos.BirthdayDto> getUpcomingBirthdays() {
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        return internProfileRepository.findByBirthdayMonth(currentMonth).stream()
                .limit(5)
                .map(i -> com.holaho.intern.shared.dto.response.DashboardDtos.BirthdayDto.builder()
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
    public com.holaho.intern.shared.dto.response.DashboardDtos.InternDashboardResponse getInternDashboard(Long userId) {
        com.holaho.intern.intern.entity.InternProfile intern = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new com.holaho.intern.shared.exception.NotFoundException("InternProfile", userId));
        Long internId = intern.getId();

        // Find active group
        java.util.Optional<com.holaho.intern.entity.GroupMember> membership = groupMemberRepository
                .findFirstByIntern_IdAndLeftAtIsNull(internId);

        int completedTasks = (int) taskRepository
                .findByAssignee_Id(internId, org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(t -> t.getStatus() == com.holaho.intern.shared.enums.TaskStatus.DONE).count();
        int allTasks = (int) taskRepository
                .findByAssignee_Id(internId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        java.time.LocalDate startDate = intern.getStartDate();
        java.time.LocalDate endDate = intern.getEndDate();

        // US: Prioritize Program dates if intern is in a group
        if (membership.isPresent()) {
            com.holaho.intern.entity.Program program = membership.get().getGroup().getProgram();
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

        return com.holaho.intern.shared.dto.response.DashboardDtos.InternDashboardResponse.builder()
                .internName(intern.getUser().getFullName())
                .position("Thực tập sinh " + (intern.getMajor() != null ? intern.getMajor() : ""))
                .mentorName(mentorName)
                .tasksCompleted(completedTasks)
                .tasksTotal(allTasks)
                .daysInternship(Math.max(0, daysInternship))
                .totalDays(totalDays)
                .recentActivities(auditLogRepository
                        .findByActorIdOrderByCreatedAtDesc(userId,
                                org.springframework.data.domain.PageRequest.of(0, 5))
                        .stream().map(log -> com.holaho.intern.shared.dto.response.DashboardDtos.ActivityDto.builder()
                                .content(log.getAction() + " on " + log.getEntityType())
                                .timeAgo(getTimeAgo(log.getCreatedAt()))
                                .color("green")
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }

    private Double calculateCompletionRate() {
        long totalInterns = internProfileRepository.count();
        if (totalInterns == 0)
            return 0.0;

        long completedInterns = internProfileRepository.countCompletedInterns(java.time.LocalDate.now());

        return ((double) completedInterns / totalInterns) * 100.0;
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "universityStats")
    public List<InternCountStatDto> getUniversityStats() {
        return internProfileRepository.countByUniversityAndMajor();
    }
}
