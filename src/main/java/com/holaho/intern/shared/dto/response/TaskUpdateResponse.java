package com.holaho.intern.shared.dto.response;

import com.holaho.intern.task.entity.TaskUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateResponse {

    private Long id;
    private Long taskId;
    private Long internId;
    private String internName;
    private Integer progressPercent;
    private String content;
    private LocalDateTime createdAt;

    /**
     * Convert TaskUpdate entity to TaskUpdateResponse DTO
     */
    public static TaskUpdateResponse from(TaskUpdate update) {
        if (update == null) {
            return null;
        }

        TaskUpdateResponse res = new TaskUpdateResponse();
        res.setId(update.getId());
        res.setProgressPercent(update.getProgressPercent());
        res.setContent(update.getContent());
        res.setCreatedAt(update.getCreatedAt());

        // Set task info
        if (update.getTask() != null) {
            res.setTaskId(update.getTask().getId());
        }

        // Set intern info
        if (update.getIntern() != null) {
            res.setInternId(update.getIntern().getId());
            if (update.getIntern().getUser() != null) {
                res.setInternName(update.getIntern().getUser().getFullName());
            }
        }

        return res;
    }
}

