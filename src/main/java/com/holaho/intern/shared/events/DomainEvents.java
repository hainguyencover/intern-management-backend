package com.holaho.intern.shared.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class DomainEvents {

    @Getter
    public static class ApplicationAcceptedEvent extends ApplicationEvent {
        private final Long applicationId;
        private final Long userId;
        private final String candidateName;
        private final String email;

        public ApplicationAcceptedEvent(Object source, Long applicationId, Long userId, String candidateName, String email) {
            super(source);
            this.applicationId = applicationId;
            this.userId = userId;
            this.candidateName = candidateName;
            this.email = email;
        }
    }

    @Getter
    public static class ApplicationRejectedEvent extends ApplicationEvent {
        private final Long applicationId;
        private final Long userId;
        private final String candidateName;
        private final String email;
        private final String reason;

        public ApplicationRejectedEvent(Object source, Long applicationId, Long userId, String candidateName, String email, String reason) {
            super(source);
            this.applicationId = applicationId;
            this.userId = userId;
            this.candidateName = candidateName;
            this.email = email;
            this.reason = reason;
        }
    }

    @Getter
    public static class InternAssignedEvent extends ApplicationEvent {
        private final Long internId;
        private final Long mentorUserId;
        private final String internName;
        private final String mentorName;

        public InternAssignedEvent(Object source, Long internId, Long mentorUserId, String internName, String mentorName) {
            super(source);
            this.internId = internId;
            this.mentorUserId = mentorUserId;
            this.internName = internName;
            this.mentorName = mentorName;
        }
    }

    @Getter
    public static class TaskAssignedEvent extends ApplicationEvent {
        private final Long taskId;
        private final Long assigneeUserId;
        private final String assigneeEmail;
        private final String assigneeName;
        private final String taskTitle;
        private final String creatorName;
        private final String dueDate;

        public TaskAssignedEvent(Object source, Long taskId, Long assigneeUserId, String assigneeEmail, String assigneeName, String taskTitle, String creatorName, String dueDate) {
            super(source);
            this.taskId = taskId;
            this.assigneeUserId = assigneeUserId;
            this.assigneeEmail = assigneeEmail;
            this.assigneeName = assigneeName;
            this.taskTitle = taskTitle;
            this.creatorName = creatorName;
            this.dueDate = dueDate;
        }
    }

    @Getter
    public static class WeeklyReportSubmittedEvent extends ApplicationEvent {
        private final Long reportId;
        private final Long mentorUserId;
        private final String internName;
        private final String weekRange;

        public WeeklyReportSubmittedEvent(Object source, Long reportId, Long mentorUserId, String internName, String weekRange) {
            super(source);
            this.reportId = reportId;
            this.mentorUserId = mentorUserId;
            this.internName = internName;
            this.weekRange = weekRange;
        }
    }

    @Getter
    public static class WeeklyReportReviewedEvent extends ApplicationEvent {
        private final Long reportId;
        private final Long internUserId;
        private final String mentorName;
        private final String assessment;

        public WeeklyReportReviewedEvent(Object source, Long reportId, Long internUserId, String mentorName, String assessment) {
            super(source);
            this.reportId = reportId;
            this.internUserId = internUserId;
            this.mentorName = mentorName;
            this.assessment = assessment;
        }
    }

    @Getter
    public static class TaskCompletedEvent extends ApplicationEvent {
        private final Long taskId;
        private final Long creatorUserId;
        private final String internName;
        private final String taskTitle;

        public TaskCompletedEvent(Object source, Long taskId, Long creatorUserId, String internName, String taskTitle) {
            super(source);
            this.taskId = taskId;
            this.creatorUserId = creatorUserId;
            this.internName = internName;
            this.taskTitle = taskTitle;
        }
    }

    @Getter
    public static class InterviewScheduledEvent extends ApplicationEvent {
        private final Long interviewId;
        private final Long candidateUserId;
        private final String candidateName;
        private final String email;
        private final String dateTime;

        public InterviewScheduledEvent(Object source, Long interviewId, Long candidateUserId, String candidateName, String email, String dateTime) {
            super(source);
            this.interviewId = interviewId;
            this.candidateUserId = candidateUserId;
            this.candidateName = candidateName;
            this.email = email;
            this.dateTime = dateTime;
        }
    }

    @Getter
    public static class PasswordChangedEvent extends ApplicationEvent {
        private final Long userId;
        private final String email;
        private final String name;

        public PasswordChangedEvent(Object source, Long userId, String email, String name) {
            super(source);
            this.userId = userId;
            this.email = email;
            this.name = name;
        }
    }
}
