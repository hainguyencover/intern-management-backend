package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.request.CreateProgramRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.ProgramResponse;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hr/programs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class HrProgramController {

    private final ProgramService programService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProgramResponse>> create(@Valid @RequestBody CreateProgramRequest req) {
        ProgramResponse response = programService.createProgram(req);
        return ResponseEntity.ok(ApiResponse.success("Tạo chương trình thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProgramResponse>>> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) ProgramStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProgramResponse> response = programService.search(departmentId, status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgramResponse>> detail(@PathVariable Long id) {
        ProgramResponse response = programService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgramResponse>> update(@PathVariable Long id,
                                                  @Valid @RequestBody CreateProgramRequest req) {
        ProgramResponse response = programService.updateProgram(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chương trình thành công", response));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ProgramResponse>> publish(@PathVariable Long id) {
        ProgramResponse response = programService.updateProgramStatus(id, ProgramStatus.ACTIVE);
        return ResponseEntity.ok(ApiResponse.success("Phát hành chương trình thành công", response));
    }
}
