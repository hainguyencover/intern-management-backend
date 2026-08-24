package com.holaho.intern.notification.listener;

import com.holaho.intern.notification.entity.MeetingReminder;
import com.holaho.intern.notification.entity.NotificationEvent;
import com.holaho.intern.notification.event.MeetingCancelledEvent;
import com.holaho.intern.notification.event.MeetingCreatedEvent;
import com.holaho.intern.notification.event.MeetingUpdatedEvent;
import com.holaho.intern.notification.repository.MeetingReminderRepository;
import com.holaho.intern.notification.repository.NotificationEventRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingNotificationListener {

    private final NotificationEventRepository eventRepository;
    private final NotificationService notificationService;
    private final MeetingReminderRepository reminderRepository;
    private final UserRepository userRepository;

    @EventListener
    @Transactional
    public void handleMeetingCreated(MeetingCreatedEvent event) {
        if (!checkAndSaveIdempotency(event.eventId().toString(), "MEETING_CREATED", "MEETING", event.meetingId())) {
            log.info("Event {} already processed, skipping.", event.eventId());
            return;
        }

        String formattedTime = event.startTime() != null 
                ? event.startTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) 
                : "tới";
        String message = String.format("Bạn có lịch họp mới: '%s' với %s lúc %s.", event.meetingTitle(), event.organizerName(), formattedTime);

        List<User> participants = userRepository.findAllById(event.participantIds());
        for (User participant : participants) {
            notificationService.createNotification(
                    participant,
                    NotificationType.MEETING_CREATED,
                    "Lịch họp mới: " + event.meetingTitle(),
                    message,
                    "MEETING",
                    event.meetingId()
            );
        }

        // Schedule 24H and 30M reminders if start time is in future
        if (event.startTime() != null && event.startTime().isAfter(LocalDateTime.now())) {
            createReminder(event.meetingId(), "24H", event.startTime().minusHours(24));
            createReminder(event.meetingId(), "30M", event.startTime().minusMinutes(30));
        }

        log.info("Dispatched MEETING_CREATED notifications for meeting ID: {}", event.meetingId());
    }

    @EventListener
    @Transactional
    public void handleMeetingUpdated(MeetingUpdatedEvent event) {
        if (!checkAndSaveIdempotency(event.eventId().toString(), "MEETING_UPDATED", "MEETING", event.meetingId())) {
            log.info("Event {} already processed, skipping.", event.eventId());
            return;
        }

        String formattedTime = event.newStartTime() != null 
                ? event.newStartTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) 
                : "mới";
        String message = String.format("Lịch họp '%s' đã thay đổi thời gian sang %s.", event.meetingTitle(), formattedTime);

        List<User> participants = userRepository.findAllById(event.participantIds());
        for (User participant : participants) {
            notificationService.createNotification(
                    participant,
                    NotificationType.MEETING_UPDATED,
                    "Thay đổi lịch họp: " + event.meetingTitle(),
                    message,
                    "MEETING",
                    event.meetingId()
            );
        }

        log.info("Dispatched MEETING_UPDATED notifications for meeting ID: {}", event.meetingId());
    }

    @EventListener
    @Transactional
    public void handleMeetingCancelled(MeetingCancelledEvent event) {
        if (!checkAndSaveIdempotency(event.eventId().toString(), "MEETING_CANCELLED", "MEETING", event.meetingId())) {
            log.info("Event {} already processed, skipping.", event.eventId());
            return;
        }

        String message = String.format("Lịch họp '%s' đã bị hủy. Lý do: %s.", event.meetingTitle(), event.reason() != null ? event.reason() : "Không có");

        List<User> participants = userRepository.findAllById(event.participantIds());
        for (User participant : participants) {
            notificationService.createNotification(
                    participant,
                    NotificationType.MEETING_CANCELLED,
                    "Lịch họp đã hủy: " + event.meetingTitle(),
                    message,
                    "MEETING",
                    event.meetingId()
            );
        }

        log.info("Dispatched MEETING_CANCELLED notifications for meeting ID: {}", event.meetingId());
    }

    private boolean checkAndSaveIdempotency(String eventId, String eventType, String aggregateType, Long aggregateId) {
        if (eventRepository.existsByEventId(eventId)) {
            return false;
        }
        NotificationEvent event = NotificationEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .processedAt(LocalDateTime.now())
                .build();
        eventRepository.save(event);
        return true;
    }

    private void createReminder(Long meetingId, String type, LocalDateTime scheduledAt) {
        if (scheduledAt.isAfter(LocalDateTime.now()) && !reminderRepository.existsByMeetingIdAndReminderType(meetingId, type)) {
            MeetingReminder reminder = MeetingReminder.builder()
                    .meetingId(meetingId)
                    .reminderType(type)
                    .scheduledAt(scheduledAt)
                    .status("PENDING")
                    .build();
            reminderRepository.save(reminder);
        }
    }
}
