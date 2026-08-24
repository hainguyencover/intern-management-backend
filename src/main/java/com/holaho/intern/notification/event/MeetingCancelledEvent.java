package com.holaho.intern.notification.event;

import java.util.List;
import java.util.UUID;

public record MeetingCancelledEvent(
    UUID eventId,
    Long meetingId,
    String meetingTitle,
    List<Long> participantIds,
    String reason
) {
}
