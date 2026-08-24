package com.holaho.intern.notification.service;

import com.holaho.intern.notification.dto.NotificationResponse;
import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.enums.NotificationChannel;
import com.holaho.intern.notification.enums.NotificationStatus;
import com.holaho.intern.notification.repository.NotificationRepository;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationDeliveryService deliveryService;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User recipient;
    private Notification notification;

    @BeforeEach
    void setUp() {
        recipient = new User();
        recipient.setId(10L);
        recipient.setEmail("intern@holaho.com");
        recipient.setFullName("Nguyen Van A");

        notification = Notification.builder()
                .recipient(recipient)
                .type(NotificationType.MEETING_CREATED)
                .title("Lịch họp mới")
                .message("Bạn có lịch họp với Mentor lúc 14:00")
                .referenceType("MEETING")
                .referenceId(55L)
                .status(NotificationStatus.UNREAD)
                .build();
        notification.setId(100L);
    }

    @Test
    @DisplayName("Create notification creates delivery records and sends WebSocket push")
    void createNotification_success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(preferenceService.isChannelEnabled(eq(10L), eq("MEETING_CREATED"), eq(NotificationChannel.IN_APP))).thenReturn(true);
        when(preferenceService.isChannelEnabled(eq(10L), eq("MEETING_CREATED"), eq(NotificationChannel.EMAIL))).thenReturn(true);

        Notification saved = notificationService.createNotification(
                recipient,
                NotificationType.MEETING_CREATED,
                "Lịch họp mới",
                "Bạn có lịch họp với Mentor lúc 14:00",
                "MEETING",
                55L
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(100L);
        verify(deliveryService).createDelivery(notification, NotificationChannel.IN_APP);
        verify(deliveryService).createDelivery(notification, NotificationChannel.EMAIL);
        verify(messagingTemplate).convertAndSendToUser(eq("intern@holaho.com"), eq("/notifications"), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("Mark notification as read updates status to READ")
    void markAsRead_success() {
        when(notificationRepository.findByIdAndRecipientId(100L, 10L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(100L, 10L);

        assertThat(response).isNotNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Mark notification as read throws NotFoundException when missing")
    void markAsRead_notFound() {
        when(notificationRepository.findByIdAndRecipientId(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Notification not found: 999");
    }

    @Test
    @DisplayName("Get unread count returns count from repository")
    void getUnreadCount_success() {
        when(notificationRepository.countByRecipientIdAndStatus(10L, NotificationStatus.UNREAD)).thenReturn(5L);

        long count = notificationService.getUnreadCount(10L);

        assertThat(count).isEqualTo(5L);
    }
}
