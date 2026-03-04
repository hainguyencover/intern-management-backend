package com.example.backend.elasticsearch.repository;

import com.example.backend.elasticsearch.model.TaskIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskSearchRepository extends ElasticsearchRepository<TaskIndex, String> {
    List<TaskIndex> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description);
}
