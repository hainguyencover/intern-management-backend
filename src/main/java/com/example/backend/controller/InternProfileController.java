package com.example.backend.controller;

import com.example.backend.dto.request.InternProfileRequest;
import com.example.backend.dto.response.InternProfileResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.service.InternProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interns")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InternProfileController {

    private final InternProfileService internProfileService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternProfileResponse> createIntern(@Valid @RequestBody InternProfileRequest request) {
        InternProfileResponse response = internProfileService.createIntern(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/me")
    public ResponseEntity<InternProfileResponse> createMyProfile(@Valid @RequestBody InternProfileRequest request) {
        InternProfileResponse response = internProfileService.createForCurrentUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<InternProfileResponse> updateIntern(@PathVariable Long id,
            @Valid @RequestBody InternProfileRequest request) {
        InternProfileResponse response = internProfileService.updateIntern(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<InternProfileResponse> updateMyProfile(@Valid @RequestBody InternProfileRequest request) {
        InternProfileResponse response = intern_profileService_updateForCurrentUser(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternProfileResponse> getInternById(@PathVariable Long id) {
        InternProfileResponse response = internProfileService.getInternById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<InternProfileResponse>> searchInterns(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String university,
            @RequestParam(required = false) String major,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<InternProfileResponse> response = internProfileService.searchInterns(
                search, university, major, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<Void> deleteIntern(@PathVariable Long id) {
        internProfileService.deleteIntern(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/universities")
    public ResponseEntity<List<String>> getAllUniversities() {
        return ResponseEntity.ok(intern_profileService_getAllUniversities());
    }

    @GetMapping("/majors")
    public ResponseEntity<List<String>> getAllMajors() {
        return ResponseEntity.ok(intern_profileService_getAllMajors());
    }

    // wrapper methods to avoid IDE inspections while preserving simple calls
    private List<String> intern_profileService_getAllUniversities() {
        return internProfileService.getAllUniversities();
    }

    private List<String> intern_profileService_getAllMajors() {
        return internProfileService.getAllMajors();
    }

    private InternProfileResponse intern_profileService_updateForCurrentUser(InternProfileRequest req) {
        return internProfileService.updateForCurrentUser(req);
    }
}
