package com.holaho.intern.shared.dto.response;

import com.holaho.intern.task.entity.Task;
import com.holaho.intern.shared.enums.TaskPriority;
import com.holaho.intern.shared.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long id;
    private Long groupId;
    private String groupName;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private Integer progressPercent;
    private LocalDateTime startDate;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private boolean overdue;
    private Integer weight;
    private Long createdBy;
    private String creatorName;
    private Long mentorId;
    private String mentorName;
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer updateCount;

    public static TaskResponse from(Task t) {
        if (t == null) return null;
        TaskResponse res = new TaskResponse();
        res.setId(t.getId());
        res.setTitle(t.getTitle());
        res.setDescription(t.getDescription());
        res.setPriority(t.getPriority());
        res.setStatus(t.getStatus());
        res.setProgressPercent(t.getProgressPercent() != null ? t.getProgressPercent() : 0);
        res.setStartDate(t.getStartDate());
        res.setDueDate(t.getDueDate());
        res.setCompletedAt(t.getCompletedAt());
        res.setWeight(t.getWeight());
        res.setCreatedAt(t.getCreatedAt());
        res.setUpdatedAt(t.getUpdatedAt());

        boolean isOverdue = t.getDueDate() != null && LocalDateTime.now().isAfter(t.getDueDate())
                && t.getStatus() != TaskStatus.APPROVED && t.getStatus() != TaskStatus.DONE && t.getStatus() != TaskStatus.CANCELLED;
        res.setOverdue(isOverdue);

        if (t.getGroup() != null) {
            res.setGroupId(t.getGroup().getId());
            res.setGroupName(t.getGroup().getName());
        }

        if (t.getCreatedBy() != null) {
            res.setCreatedBy(t.getCreatedBy().getId());
            res.setCreatorName(t.getCreatedBy().getFullName());
        }

        if (t.getMentor() != null) {
            res.setMentorId(t.getMentor().getId());
            res.setMentorName(t.getMentor().getFullName());
        }

        if (t.getAssignee() != null) {
            res.setAssigneeId(t.getAssignee().getId());
            if (t.getAssignee().getUser() != null) {
                res.setAssigneeName(t.getAssignee().getUser().getFullName());
            }
        }

        return res;
    }
}


