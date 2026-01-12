package com.example.backend.controller;

import com.example.backend.dto.request.MentorCreateRequest;
import com.example.backend.dto.response.MentorResponseDto;
import com.example.backend.service.MentorService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mentors")
public class MentorController {

    private final MentorService mentorService;

    public MentorController(MentorService mentorService) {
        this.mentorService = mentorService;
    }

    // POST /api/mentors
    @PostMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<MentorResponseDto> createMentor(@RequestBody MentorCreateRequest req) {
        MentorResponseDto created = mentorService.createMentor(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ✅ FE đang gọi: GET /api/mentors?size=200
    @GetMapping
    public Page<MentorResponseDto> getMentors(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return mentorService.getMentors(page, size);
    }

    // GET /api/mentors/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<MentorResponseDto> getMentorById(@PathVariable Long id) {
        MentorResponseDto mentor = mentorService.getMentorById(id);
        return ResponseEntity.ok(mentor);
    }
}
