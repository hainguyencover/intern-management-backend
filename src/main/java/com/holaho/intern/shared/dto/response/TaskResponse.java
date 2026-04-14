package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.Task;
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
    private LocalDateTime dueDate;
    private TaskStatus status;
    private Long createdBy;
    private String creatorName;
    private Integer progressPercent;
    private LocalDateTime createdAt;
    private Integer updateCount;
    private Long assigneeId;
    private String assigneeName;

    public static TaskResponse from(Task t) {
        TaskResponse res = new TaskResponse();
        res.setId(t.getId());
        res.setTitle(t.getTitle());
        res.setDescription(t.getDescription());
        res.setDueDate(t.getDueDate());
        res.setStatus(t.getStatus());
        res.setCreatedAt(t.getCreatedAt());

        if (t.getGroup() != null) {
            res.setGroupId(t.getGroup().getId());
            res.setGroupName(t.getGroup().getName());
        }

        if (t.getCreatedBy() != null) {
            res.setCreatedBy(t.getCreatedBy().getId());
            res.setCreatorName(t.getCreatedBy().getFullName());
        }

        return res;
    }
}

