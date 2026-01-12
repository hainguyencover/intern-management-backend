package com.example.backend.controller.hr;

import com.example.backend.dto.request.ProgramCreateRequest;
import com.example.backend.dto.request.ProgramUpdateRequest;
import com.example.backend.dto.response.ProgramResponse;
import com.example.backend.enums.ProgramStatus;
import com.example.backend.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr/programs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class HrProgramController {

    private final ProgramService programService;

    @PostMapping
    public ResponseEntity<ProgramResponse> create(@Valid @RequestBody ProgramCreateRequest req) {
        return ResponseEntity.ok(programService.createProgram(req));
    }

    @GetMapping
    public ResponseEntity<Page<ProgramResponse>> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) ProgramStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(programService.listPrograms(departmentId, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(programService.getProgram(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProgramResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ProgramUpdateRequest req) {
        return ResponseEntity.ok(programService.updateProgram(id, req));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ProgramResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(programService.publishProgram(id));
    }
}
