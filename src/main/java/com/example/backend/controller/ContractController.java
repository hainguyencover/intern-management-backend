package com.example.backend.controller;

import com.example.backend.dto.response.ContractResponse;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ContractResponse> upload(
            @RequestParam Long applicationId,
            @RequestParam("file") MultipartFile file) {
        ContractResponse response = contractService.uploadContract(applicationId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/sign")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ContractResponse> sign(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ContractResponse response = contractService.signContract(id, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<List<ContractResponse>> getMyContracts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ContractResponse> response = contractService.getMyContracts(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ContractResponse> get(@PathVariable Long id) {
        ContractResponse response = contractService.getContract(id);
        return ResponseEntity.ok(response);
    }
}
