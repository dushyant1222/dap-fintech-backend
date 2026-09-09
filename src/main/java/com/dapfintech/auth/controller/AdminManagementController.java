package com.dapfintech.auth.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.auth.dto.request.CreateAdminRequest;
import com.dapfintech.auth.dto.response.AdminResponse;
import com.dapfintech.auth.service.AdminManagementService;
import com.dapfintech.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminResponse>> createAdmin(@RequestBody CreateAdminRequest request) {
        AdminResponse response = adminManagementService.createAdmin(request);
        return ResponseEntity.ok(ApiResponse.<AdminResponse>builder()
                .success(true)
                .message("Admin created successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminResponse>>> getAllAdmins() {
        List<AdminResponse> response = adminManagementService.getAllAdmins();
        return ResponseEntity.ok(ApiResponse.<List<AdminResponse>>builder()
                .success(true)
                .message("Admins fetched successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{adminId}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleAdminStatus(@PathVariable UUID adminId) {
        adminManagementService.toggleAdminStatus(adminId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Admin status updated successfully")
                .data(null)
                .build());
    }
}
