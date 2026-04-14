package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.ContractResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractResponse>> upload(
            @RequestParam Long applicationId,
            @RequestParam("file") MultipartFile file) {
        ContractResponse response = contractService.uploadContract(applicationId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("HÃ¡Â»Â£p Ã„â€˜Ã¡Â»â€œng Ã„â€˜Ã†Â°Ã¡Â»Â£c tÃ¡ÂºÂ£i lÃƒÂªn thÃƒÂ nh cÃƒÂ´ng", response));
    }

    @PutMapping("/{id}/sign")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<ContractResponse>> sign(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ContractResponse response = contractService.signContract(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("KÃƒÂ½ hÃ¡Â»Â£p Ã„â€˜Ã¡Â»â€œng thÃƒÂ nh cÃƒÂ´ng", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getMyContracts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ContractResponse> response = contractService.getMyContracts(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<ContractResponse>> get(@PathVariable Long id) {
        ContractResponse response = contractService.getContract(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

