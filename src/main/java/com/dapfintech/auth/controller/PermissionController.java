package com.dapfintech.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.auth.dto.response.PermissionResponse;
import com.dapfintech.auth.service.PermissionService;
import com.dapfintech.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {

        List<PermissionResponse> permissions = permissionService.getAllPermissions();

        return ResponseEntity.ok(
                ApiResponse.<List<PermissionResponse>> builder()
                        .success(true)
                        .message("Permissions fetched successfully")
                        .data(permissions)
                        .build()
        );
    }
}