package com.example.backend.controller;

import com.example.backend.dto.request.CreateMeetingRequest;
import com.example.backend.dto.response.MeetingResponse;
import com.example.backend.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @PostMapping
    public MeetingResponse create(@RequestBody CreateMeetingRequest req) {
        return meetingService.createMeeting(req);
    }

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @GetMapping("/group/{groupId}")
    public List<MeetingResponse> getByGroup(@PathVariable Long groupId) {
        return meetingService.getMeetingsByGroup(groupId);
    }
}
