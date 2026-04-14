package com.holaho.intern.service;

import com.holaho.intern.task.entity.Task;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.shared.dto.response.ScheduleEventResponse;


import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.intern.repository.InternProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

        private final InternProfileRepository internProfileRepository;
        private final com.holaho.intern.task.repository.TaskRepository taskRepository;

        @Transactional(readOnly = true)
        public java.util.List<com.holaho.intern.shared.dto.response.ScheduleEventResponse> getMySchedule(String email) {

                InternProfile intern = internProfileRepository.findByUser_Email(email)
                                .orElseThrow(() -> new NotFoundException(
                                                "Intern profile not found for user: " + email));

                // Fetch tasks assigned to the intern
                // Using Pageable.unpaged() to get all tasks
                java.util.List<com.holaho.intern.task.entity.Task> tasks = taskRepository
                                .findByAssignee_Id(intern.getId(), org.springframework.data.domain.Pageable.unpaged())
                                .getContent();

                return tasks.stream()
                                .map(task -> com.holaho.intern.shared.dto.response.ScheduleEventResponse.builder()
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

