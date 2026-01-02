package com.example.backend.controller;

import com.example.backend.dto.response.ProgramResponse;
import com.example.backend.dto.request.ProgramUpsertRequest;
import com.example.backend.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @PreAuthorize("hasRole('HR')")
    @PostMapping
    public ProgramResponse create(@Valid @RequestBody ProgramUpsertRequest req) {
        return programService.create(req);
    }

    @PreAuthorize("hasRole('HR')")
    @PutMapping("/{id}")
    public ProgramResponse update(@PathVariable Long id, @Valid @RequestBody ProgramUpsertRequest req) {
        return programService.update(id, req);
    }

    @PreAuthorize("hasAnyRole('HR','MENTOR','INTERN','ADMIN')")
    @GetMapping("/{id}")
    public ProgramResponse get(@PathVariable Long id) {
        return programService.get(id);
    }

    @PreAuthorize("hasAnyRole('HR','MENTOR','INTERN','ADMIN')")
    @GetMapping
    public List<ProgramResponse> list() {
        return programService.list();
    }
}
