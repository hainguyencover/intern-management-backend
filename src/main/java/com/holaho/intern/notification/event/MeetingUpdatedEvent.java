package com.holaho.intern.notification.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MeetingUpdatedEvent(
    UUID eventId,
    Long meetingId,
    String meetingTitle,
    List<Long> participantIds,
    LocalDateTime newStartTime,
    String newLocation
) {
}
