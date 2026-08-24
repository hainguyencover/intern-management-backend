package com.holaho.intern.notification.repository;

import com.holaho.intern.notification.entity.NotificationDelivery;
import com.holaho.intern.notification.enums.DeliveryStatus;
import com.holaho.intern.notification.enums.NotificationChannel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findByStatusAndChannel(
        DeliveryStatus status, NotificationChannel channel, Pageable pageable
    );

    List<NotificationDelivery> findByNotificationId(Long notificationId);
}
