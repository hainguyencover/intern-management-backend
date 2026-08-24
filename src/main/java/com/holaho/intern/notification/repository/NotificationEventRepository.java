package com.holaho.intern.notification.repository;

import com.holaho.intern.notification.entity.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    boolean existsByEventId(String eventId);

    Optional<NotificationEvent> findByEventId(String eventId);
}
