package com.example.backend.elasticsearch.service;

import com.example.backend.elasticsearch.model.InternIndex;
import com.example.backend.elasticsearch.model.TaskIndex;
import com.example.backend.elasticsearch.repository.InternSearchRepository;
import com.example.backend.elasticsearch.repository.TaskSearchRepository;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchSearchService implements SearchService {

    private final InternSearchRepository internSearchRepository;
    private final TaskSearchRepository taskSearchRepository;

    public void indexIntern(InternProfile intern) {
        InternIndex index = InternIndex.builder()
                .id(intern.getId().toString())
                .fullName(intern.getUser().getFullName())
                .email(intern.getUser().getEmail())
                .studentCode(intern.getStudentCode())
                .university(intern.getUniversity())
                .major(intern.getMajor())
                .gpa(intern.getGpa())
                .build();
        internSearchRepository.save(index);
        log.info("Indexed intern in Elasticsearch: {}", intern.getId());
    }

    public void indexTask(Task task) {
        TaskIndex index = TaskIndex.builder()
                .id(task.getId().toString())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getUser().getFullName() : null)
                .build();
        taskSearchRepository.save(index);
        log.info("Indexed task in Elasticsearch: {}", task.getId());
    }

    public void deleteIntern(Long internId) {
        internSearchRepository.deleteById(internId.toString());
        log.info("Deleted intern from Elasticsearch: {}", internId);
    }

    public void deleteTask(Long taskId) {
        taskSearchRepository.deleteById(taskId.toString());
        log.info("Deleted task from Elasticsearch: {}", taskId);
    }

    public List<InternIndex> searchInterns(String query) {
        return internSearchRepository
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrStudentCodeContainingIgnoreCase(
                        query, query, query);
    }

    public List<TaskIndex> searchTasks(String query) {
        return taskSearchRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                query, query);
    }
}
