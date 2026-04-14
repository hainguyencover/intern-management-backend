package com.holaho.intern.shared.elasticsearch.service;

import com.holaho.intern.shared.elasticsearch.model.InternIndex;
import com.holaho.intern.shared.elasticsearch.model.TaskIndex;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.task.entity.Task;
import java.util.List;

public interface SearchService {
    void indexIntern(InternProfile intern);

    void indexTask(Task task);

    void deleteIntern(Long internId);

    void deleteTask(Long taskId);

    List<InternIndex> searchInterns(String query);

    List<TaskIndex> searchTasks(String query);
}

