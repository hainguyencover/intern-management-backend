package com.holaho.intern.university.service;

import com.holaho.intern.exception.ResourceNotFoundException;
import com.holaho.intern.exception.UnauthorizedException;
import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.entity.UniversityNotificationPreference;
import com.holaho.intern.notification.enums.NotificationStatus;
import com.holaho.intern.notification.repository.NotificationRepository;
import com.holaho.intern.notification.repository.UniversityNotificationPreferenceRepository;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.UniversityNotificationPreferenceUpdateRequest;
import com.holaho.intern.university.dto.UniversityNotificationResponse;
import com.holaho.intern.university.dto.UniversityNotificationUnreadCountResponse;
import com.holaho.intern.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UniversityNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UniversityNotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private UniversityNotificationService service;

    private CustomUserDetails mockPrincipal;
    private Notification mockNotification;
    private UniversityNotificationPreference mockPreference;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(100L);
        user.setEmail("admin@fpt.edu.vn");

        mockPrincipal = new CustomUserDetails(user, List.of(), 1L, null);

        mockNotification = Notification.builder()
                .universityId(1L)
                .type(NotificationType.INTERNSHIP_COMPLETED)
                .title("Sinh viên hoàn thành thực tập")
                .message("Nguyễn Văn A đã hoàn thành thực tập.")
                .status(NotificationStatus.UNREAD)
                .build();
        mockNotification.setId(50L);

        mockPreference = UniversityNotificationPreference.builder()
                .universityId(1L)
                .internshipCompletedEnabled(true)
                .internshipTerminatedEnabled(true)
                .emailEnabled(true)
                .inAppEnabled(true)
                .build();
        mockPreference.setId(10L);
    }

    @Test
    void findNotifications_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(mockNotification));

        when(notificationRepository.findByUniversityIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(page);

        Page<UniversityNotificationResponse> result = service.findNotifications(mockPrincipal, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Sinh viên hoàn thành thực tập", result.getContent().get(0).getTitle());
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationRepository.countByUniversityIdAndStatus(1L, NotificationStatus.UNREAD))
                .thenReturn(5L);

        UniversityNotificationUnreadCountResponse response = service.getUnreadCount(mockPrincipal);

        assertNotNull(response);
        assertEquals(5L, response.getCount());
    }

    @Test
    void getNotificationDetail_Success() {
        when(notificationRepository.findByIdAndUniversityId(50L, 1L))
                .thenReturn(Optional.of(mockNotification));

        UniversityNotificationResponse response = service.getNotificationDetail(mockPrincipal, 50L);

        assertNotNull(response);
        assertEquals(50L, response.getId());
    }

    @Test
    void getNotificationDetail_NotFound_ThrowsException() {
        when(notificationRepository.findByIdAndUniversityId(99L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getNotificationDetail(mockPrincipal, 99L));
    }

    @Test
    void markAsRead_Success() {
        when(notificationRepository.findByIdAndUniversityId(50L, 1L))
                .thenReturn(Optional.of(mockNotification));

        UniversityNotificationResponse response = service.markAsRead(mockPrincipal, 50L);

        assertNotNull(response);
        assertEquals(NotificationStatus.READ, response.getStatus());
        verify(notificationRepository, times(1)).save(mockNotification);
    }

    @Test
    void markAllAsRead_Success() {
        when(notificationRepository.markAllAsReadForUniversity(eq(1L), any(LocalDateTime.class)))
                .thenReturn(3);

        int count = service.markAllAsRead(mockPrincipal);

        assertEquals(3, count);
    }

    @Test
    void updatePreference_Success() {
        when(preferenceRepository.findByUniversityId(1L))
                .thenReturn(Optional.of(mockPreference));
        when(preferenceRepository.save(any(UniversityNotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UniversityNotificationPreferenceUpdateRequest request = UniversityNotificationPreferenceUpdateRequest.builder()
                .emailEnabled(false)
                .build();

        var result = service.updatePreference(mockPrincipal, request);

        assertNotNull(result);
        assertFalse(result.isEmailEnabled());
    }

    @Test
    void nullUniversityId_ThrowsUnauthorized() {
        User user = new User();
        user.setId(200L);
        CustomUserDetails nullUnivPrincipal = new CustomUserDetails(user, List.of(), null, null);

        assertThrows(UnauthorizedException.class, () -> service.getUnreadCount(nullUnivPrincipal));
    }
}
