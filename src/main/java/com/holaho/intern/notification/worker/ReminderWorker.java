package com.holaho.intern.notification.worker;

import com.holaho.intern.notification.entity.MeetingReminder;
import com.holaho.intern.notification.repository.MeetingReminderRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderWorker {

    private final MeetingReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processDueReminders() {
        List<MeetingReminder> dueReminders = reminderRepository.findDueReminders(LocalDateTime.now(), PageRequest.of(0, 50));

        if (dueReminders.isEmpty()) {
            return;
        }

        log.info("Processing {} due meeting reminders...", dueReminders.size());

        for (MeetingReminder reminder : dueReminders) {
            try {
                // Fetch all active users (or meeting participants)
                List<User> users = userRepository.findAll();
                String reminderText = "24H".equalsIgnoreCase(reminder.getReminderType()) ? "24 giờ" : "30 phút";
                String title = String.format("Nhắc lịch họp (%s trước)", reminderText);
                String message = String.format("Lịch họp #%d của bạn sẽ diễn ra trong %s nữa. Vui lòng chuẩn bị tham gia.", reminder.getMeetingId(), reminderText);

                for (User user : users) {
                    notificationService.createNotification(
                            user,
                            NotificationType.MEETING_REMINDER,
                            title,
                            message,
                            "MEETING",
                            reminder.getMeetingId()
                    );
                }

                reminder.setStatus("SENT");
                reminder.setSentAt(LocalDateTime.now());
                reminderRepository.save(reminder);
            } catch (Exception ex) {
                log.error("Failed to process meeting reminder ID {}: {}", reminder.getId(), ex.getMessage());
            }
        }
    }
}
