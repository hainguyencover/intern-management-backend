package com.example.backend.controller;

import com.example.backend.dto.request.CreateLeaveRequest;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.LeaveRequest;
import com.example.backend.enums.LeaveStatus;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interns/me/leave-requests")
public class InternLeaveController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final InternProfileRepository internProfileRepository;

    @PostMapping
    @PreAuthorize("hasRole('INTERN')")
    public Map<String, Object> create(@RequestBody CreateLeaveRequest req) {
        if (req.getFromDate() == null || req.getToDate() == null || req.getType() == null) {
            throw new BadRequestException("fromDate/toDate/type are required");
        }
        if (req.getToDate().isBefore(req.getFromDate())) {
            throw new BadRequestException("toDate must be >= fromDate");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        InternProfile intern = internProfileRepository.findByUser_Email(email)
                .orElseThrow(() -> new NotFoundException("Intern profile not found"));

        LeaveRequest lr = new LeaveRequest();
        lr.setIntern(intern); // giữ đúng như entity LeaveRequest của m đang dùng: setIntern(...)
        lr.setFromDate(req.getFromDate());
        lr.setToDate(req.getToDate());
        lr.setType(req.getType());
        lr.setReason(req.getReason());
        lr.setStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestRepository.save(lr);

        // Trả response gọn -> không dính lazy load User.roles
        Map<String, Object> res = new HashMap<>();
        res.put("id", saved.getId());
        res.put("internId", intern.getId());
        res.put("fromDate", saved.getFromDate());
        res.put("toDate", saved.getToDate());
        res.put("type", saved.getType());
        res.put("reason", saved.getReason());
        res.put("status", saved.getStatus());
        res.put("createdAt", saved.getCreatedAt()); // nếu BaseEntity có createdAt
        return res;
    }
}
