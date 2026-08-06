package com.holaho.intern.service.impl;

import com.holaho.intern.shared.dto.response.TaskResponse;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.shared.mapper.TaskMapper;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.task.service.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
    }

    @Test
    void testGetTaskById_Success() {
        TaskResponse response = new TaskResponse();
        response.setId(1L);
        response.setTitle("Test Task");

        when(taskRepository.findByIdWithGroup(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(any(Task.class))).thenReturn(response);

        TaskResponse result = taskService.getTaskById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Task", result.getTitle());
    }
}
