package com.example.backend.controller;

import com.example.backend.dto.request.AllowanceCreateRequest;
import com.example.backend.dto.response.AllowanceResponse;
import com.example.backend.entity.Allowance;
import com.example.backend.enums.AllowanceStatus;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.AllowanceService;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/allowances")
@RequiredArgsConstructor
public class AllowanceController {

    private final AllowanceService allowanceService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<AllowanceResponse> createAllowance(
            @Valid @RequestBody AllowanceCreateRequest request) {
        Allowance allowance = allowanceService.createAllowance(
                request.getInternId(),
                request.getAmount(),
                request.getAllowanceMonth(),
                request.getNotes());
        return ResponseEntity.ok(AllowanceResponse.from(allowance));
    }

    @PutMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<AllowanceResponse> markAsPaid(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        Allowance allowance = allowanceService.markAsPaid(id, user.getId());
        return ResponseEntity.ok(AllowanceResponse.from(allowance));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<AllowanceResponse> updateAllowance(
            @PathVariable Long id,
            @RequestBody AllowanceCreateRequest request) {
        // Reusing CreateRequest which has amount and notes
        Allowance allowance = allowanceService.updateAllowance(id, request.getAmount(), request.getNotes());
        return ResponseEntity.ok(AllowanceResponse.from(allowance));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<Page<AllowanceResponse>> getMyAllowances(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 10, sort = "allowanceMonth") Pageable pageable) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        Page<Allowance> page = allowanceService.getInternAllowances(internId, pageable);
        return ResponseEntity.ok(page.map(AllowanceResponse::from));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Page<AllowanceResponse>> searchAllowances(
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) AllowanceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthTo,
            @PageableDefault(size = 10, sort = "allowanceMonth") Pageable pageable) {
        Page<Allowance> page = allowanceService.searchAllowances(
                internId, status, monthFrom, monthTo, pageable);
        return ResponseEntity.ok(page.map(AllowanceResponse::from));
    }
}
