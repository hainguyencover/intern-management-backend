package com.holaho.intern.shared.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ApplicationSubmittedEvent extends ApplicationEvent {

    private final Long applicationId;
    private final Long userId;
    private final String fullName;
    private final String email;
    private final String programName;

    public ApplicationSubmittedEvent(Object source, Long applicationId, Long userId, String fullName, String email, String programName) {
        super(source);
        this.applicationId = applicationId;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.programName = programName;
    }
}
