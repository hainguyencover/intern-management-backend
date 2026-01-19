package com.example.backend.controller;

import com.example.backend.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<com.example.backend.dto.response.DepartmentResponse>> getAll() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.example.backend.dto.response.DepartmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    public ResponseEntity<com.example.backend.dto.response.DepartmentResponse> create(
            @Valid @RequestBody com.example.backend.dto.request.DepartmentRequest req) {
        return ResponseEntity.ok(departmentService.createDepartment(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<com.example.backend.dto.response.DepartmentResponse> update(@PathVariable Long id,
            @Valid @RequestBody com.example.backend.dto.request.DepartmentRequest req) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
