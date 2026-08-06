package com.holaho.intern.notification.scheduler;

import com.holaho.intern.task.entity.Task;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.service.EmailService;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final TaskRepository taskRepository;
    private final InternProfileRepository internRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    /**
     * Run daily at 08:00 AM to check for overdue tasks and remind assignees
     * Cron expression: "0 0 8 * * *" (Seconds, Minutes, Hours, Day of Month, Month, Day of Week)
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void remindOverdueTasks() {
        log.info("Running daily overdue task reminders...");
        List<Task> overdueTasks = taskRepository.findOverdueTasks(LocalDateTime.now());
        for (Task task : overdueTasks) {
            if (task.getAssignee() != null && task.getStatus() != TaskStatus.DONE) {
                Long userId = task.getAssignee().getUser().getId();
                String email = task.getAssignee().getUser().getEmail();
                String name = task.getAssignee().getUser().getFullName();

                notificationService.createNotification(
                        userId,
                        NotificationType.TASK,
                        "Nhắc nhở: Nhiệm vụ quá hạn - " + task.getTitle(),
                        "Nhiệm vụ '" + task.getTitle() + "' đã quá hạn. Vui lòng hoàn thành ngay."
                );

                emailService.sendSimpleEmail(
                        email,
                        "Nhắc nhở: Nhiệm vụ quá hạn - " + task.getTitle(),
                        "<p>Xin chào " + name + ", công việc <strong>" + task.getTitle() + "</strong> đã quá hạn chót. Vui lòng hoàn thành sớm nhất.</p>"
                );
            }
        }
    }

    /**
     * Run every Friday at 15:00 (3 PM) to remind interns to submit weekly reports
     * Cron expression: "0 0 15 * * FRI"
     */
    @Scheduled(cron = "0 0 15 * * FRI")
    public void remindWeeklyReportSubmission() {
        log.info("Running Friday weekly report reminders...");
        List<InternProfile> interns = internRepository.findAll();
        for (InternProfile intern : interns) {
            if (intern.getUser() != null) {
                Long userId = intern.getUser().getId();
                String email = intern.getUser().getEmail();
                String name = intern.getUser().getFullName();

                notificationService.createNotification(
                        userId,
                        NotificationType.SYSTEM,
                        "Nhắc nhở: Nộp báo cáo tuần thực tập",
                        "Bạn chưa nộp Báo cáo tuần này. Vui lòng hoàn thành báo cáo trước 17:00 chiều nay."
                );

                emailService.sendSimpleEmail(
                        email,
                        "Nhắc nhở: Nộp báo cáo thực tập tuần",
                        "<p>Xin chào " + name + ", vui lòng hoàn thành và nộp báo cáo tuần thực tập trên hệ thống trước 17:00 hôm nay.</p>"
                );
            }
        }
    }
}
