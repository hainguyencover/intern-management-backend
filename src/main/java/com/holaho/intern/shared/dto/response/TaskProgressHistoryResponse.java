package com.holaho.intern.shared.dto.response;

import com.holaho.intern.task.entity.TaskProgressHistory;
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
public class TaskProgressHistoryResponse {
    private Long id;
    private Long taskId;
    private Integer oldProgress;
    private Integer newProgress;
    private TaskStatus oldStatus;
    private TaskStatus newStatus;
    private String note;
    private Long changedById;
    private String changedByName;
    private LocalDateTime changedAt;

    public static TaskProgressHistoryResponse from(TaskProgressHistory history) {
        if (history == null) return null;
        TaskProgressHistoryResponse res = new TaskProgressHistoryResponse();
        res.setId(history.getId());
        res.setTaskId(history.getTask() != null ? history.getTask().getId() : null);
        res.setOldProgress(history.getOldProgress());
        res.setNewProgress(history.getNewProgress());
        res.setOldStatus(history.getOldStatus());
        res.setNewStatus(history.getNewStatus());
        res.setNote(history.getNote());
        res.setChangedAt(history.getChangedAt());

        if (history.getChangedBy() != null) {
            res.setChangedById(history.getChangedBy().getId());
            res.setChangedByName(history.getChangedBy().getFullName());
        }

        return res;
    }
}
