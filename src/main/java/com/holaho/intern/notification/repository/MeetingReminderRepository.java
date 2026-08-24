package com.holaho.intern.notification.repository;

import com.holaho.intern.notification.entity.MeetingReminder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingReminderRepository extends JpaRepository<MeetingReminder, Long> {

    boolean existsByMeetingIdAndReminderType(Long meetingId, String reminderType);

    @Query("SELECT r FROM MeetingReminder r WHERE r.status = 'PENDING' AND r.scheduledAt <= :now")
    List<MeetingReminder> findDueReminders(@Param("now") LocalDateTime now, Pageable pageable);
}
