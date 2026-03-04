package com.example.backend.elasticsearch.service;

import com.example.backend.elasticsearch.model.InternIndex;
import com.example.backend.elasticsearch.model.TaskIndex;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Task;
import java.util.List;

public interface SearchService {
    void indexIntern(InternProfile intern);

    void indexTask(Task task);

    void deleteIntern(Long internId);

    void deleteTask(Long taskId);

    List<InternIndex> searchInterns(String query);

    List<TaskIndex> searchTasks(String query);
}
