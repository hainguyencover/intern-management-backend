package com.example.backend.controller;

import com.example.backend.entity.LeaveRequest;
import com.example.backend.enums.LeaveStatus;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/leave-requests")
public class HrLeaveController {

    private final LeaveRequestRepository leaveRequestRepository;

    @GetMapping
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public List<Map<String, Object>> list(@RequestParam(required = false) LeaveStatus status) {

        List<LeaveRequest> data = leaveRequestRepository.findAll();
        if (status != null) {
            data = data.stream().filter(x -> x.getStatus() == status).toList();
        }

        return data.stream().map(this::toMap).toList();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public Map<String, Object> approve(@PathVariable Long id) {
        LeaveRequest lr = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));

        lr.setStatus(LeaveStatus.APPROVED);
        lr.setApprovedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(lr);
        return toMap(saved);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public Map<String, Object> reject(@PathVariable Long id) {
        LeaveRequest lr = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));

        lr.setStatus(LeaveStatus.REJECTED);
        lr.setApprovedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(lr);
        return toMap(saved);
    }

    private Map<String, Object> toMap(LeaveRequest lr) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", lr.getId());
        // chú ý: intern trong LeaveRequest của m đang dùng setIntern(...) => getter là getIntern()
        res.put("internId", lr.getIntern() != null ? lr.getIntern().getId() : null);
        res.put("fromDate", lr.getFromDate());
        res.put("toDate", lr.getToDate());
        res.put("type", lr.getType());
        res.put("reason", lr.getReason());
        res.put("status", lr.getStatus());
        res.put("approvedAt", lr.getApprovedAt());
        res.put("createdAt", lr.getCreatedAt());
        res.put("updatedAt", lr.getUpdatedAt());
        return res;
    }
}
