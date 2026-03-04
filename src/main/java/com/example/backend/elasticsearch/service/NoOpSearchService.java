package com.example.backend.elasticsearch.service;

import com.example.backend.elasticsearch.model.InternIndex;
import com.example.backend.elasticsearch.model.TaskIndex;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSearchService implements SearchService {

    @Override
    public void indexIntern(InternProfile intern) {
        log.debug("Elasticsearch is disabled. Skipping indexing for intern: {}", intern.getId());
    }

    @Override
    public void indexTask(Task task) {
        log.debug("Elasticsearch is disabled. Skipping indexing for task: {}", task.getId());
    }

    @Override
    public void deleteIntern(Long internId) {
        log.debug("Elasticsearch is disabled. Skipping deletion for intern: {}", internId);
    }

    @Override
    public void deleteTask(Long taskId) {
        log.debug("Elasticsearch is disabled. Skipping deletion for task: {}", taskId);
    }

    @Override
    public List<InternIndex> searchInterns(String query) {
        log.warn("Elasticsearch is disabled. Search will return empty results.");
        return Collections.emptyList();
    }

    @Override
    public List<TaskIndex> searchTasks(String query) {
        log.warn("Elasticsearch is disabled. Search will return empty results.");
        return Collections.emptyList();
    }
}
