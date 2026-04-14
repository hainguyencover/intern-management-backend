package com.holaho.intern.controller;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final InternProfileRepository internProfileRepository;
    private final TaskRepository taskRepository;

    @GetMapping("/interns/excel")
    public ResponseEntity<byte[]> exportInternsExcel() throws IOException {
        List<InternProfile> interns = internProfileRepository.findAll();
        byte[] data = exportService.exportInternsToExcel(interns);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=interns.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/tasks/pdf")
    public ResponseEntity<byte[]> exportTasksPdf() {
        List<Task> tasks = taskRepository.findAll();
        byte[] data = exportService.exportTasksToPdf(tasks);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tasks.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}

