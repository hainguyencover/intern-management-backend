package com.example.backend.controller;

import com.example.backend.entity.Department;
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
    public ResponseEntity<List<Department>> getAll() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @PostMapping
    public ResponseEntity<Department> create(@Valid @RequestBody com.example.backend.dto.request.DepartmentRequest req) {
        return ResponseEntity.ok(departmentService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> update(@PathVariable Long id,
                                             @Valid @RequestBody com.example.backend.dto.request.DepartmentRequest req) {
        return ResponseEntity.ok(departmentService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
