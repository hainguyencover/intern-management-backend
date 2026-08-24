package com.holaho.intern.notification.service;

import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.entity.NotificationDelivery;
import com.holaho.intern.notification.enums.DeliveryStatus;
import com.holaho.intern.notification.enums.NotificationChannel;
import com.holaho.intern.notification.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService {

    private final NotificationDeliveryRepository deliveryRepository;

    @Transactional
    public NotificationDelivery createDelivery(Notification notification, NotificationChannel channel) {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .notification(notification)
                .channel(channel)
                .status(DeliveryStatus.PENDING)
                .attemptCount(0)
                .build();
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public void markProcessing(NotificationDelivery delivery) {
        delivery.setStatus(DeliveryStatus.PROCESSING);
        delivery.setLastAttemptAt(LocalDateTime.now());
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        deliveryRepository.save(delivery);
    }

    @Transactional
    public void markSent(NotificationDelivery delivery) {
        delivery.setStatus(DeliveryStatus.SENT);
        delivery.setSentAt(LocalDateTime.now());
        delivery.setErrorMessage(null);
        deliveryRepository.save(delivery);
    }

    @Transactional
    public void markFailed(NotificationDelivery delivery, String error) {
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setErrorMessage(error);
        deliveryRepository.save(delivery);
    }

    @Transactional(readOnly = true)
    public List<NotificationDelivery> getDeliveriesForNotification(Long notificationId) {
        return deliveryRepository.findByNotificationId(notificationId);
    }
}
