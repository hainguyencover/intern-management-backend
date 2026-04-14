package com.holaho.intern.shared.elasticsearch.repository;

import com.holaho.intern.shared.elasticsearch.model.TaskIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskSearchRepository extends ElasticsearchRepository<TaskIndex, String> {
    List<TaskIndex> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description);
}

