package com.example.backend.service;

import com.example.backend.entity.InternProfile;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.InternProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

        private final InternProfileRepository internProfileRepository;
        private final com.example.backend.repository.TaskRepository taskRepository;

        @Transactional(readOnly = true)
        public java.util.List<com.example.backend.dto.response.ScheduleEventResponse> getMySchedule(String email) {

                InternProfile intern = internProfileRepository.findByUser_Email(email)
                                .orElseThrow(() -> new NotFoundException(
                                                "Intern profile not found for user: " + email));

                // Fetch tasks assigned to the intern
                // Using Pageable.unpaged() to get all tasks
                java.util.List<com.example.backend.entity.Task> tasks = taskRepository
                                .findByAssignee_Id(intern.getId(), org.springframework.data.domain.Pageable.unpaged())
                                .getContent();

                return tasks.stream()
                                .map(task -> com.example.backend.dto.response.ScheduleEventResponse.builder()
                                                .id(task.getId())
                                                .title(task.getTitle())
                                                .date(task.getDueDate() != null
                                                                ? task.getDueDate().toLocalDate().toString()
                                                                : null)
                                                .type("TASK")
                                                .status(task.getStatus().name())
                                                .build())
                                .filter(e -> e.getDate() != null) // Filter out tasks without date
                                .collect(java.util.stream.Collectors.toList());
        }
}
