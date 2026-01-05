package com.example.backend.controller;

import com.example.backend.dto.request.MentorCreateRequest;
import com.example.backend.dto.response.MentorResponse;
import com.example.backend.service.MentorService;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<MentorResponse> createMentor(@RequestBody MentorCreateRequest req) {
        MentorResponse created = mentorService.createMentor(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
