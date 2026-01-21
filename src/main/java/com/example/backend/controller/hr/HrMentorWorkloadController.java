package com.example.backend.controller.hr;

import com.example.backend.dto.response.MentorWorkloadResponse;
import com.example.backend.service.MentorService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr/mentors")
public class HrMentorWorkloadController {

    private final MentorService mentorService;

    public HrMentorWorkloadController(MentorService mentorService) {
        this.mentorService = mentorService;
    }

    @GetMapping("/workload")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public Page<MentorWorkloadResponse> workload(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search
    ) {
        return mentorService.getMentorWorkload(page, size, search);
    }
}
