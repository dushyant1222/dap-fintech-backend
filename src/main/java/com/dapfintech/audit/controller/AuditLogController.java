package com.dapfintech.audit.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.audit.dto.response.AuditLogResponse;
import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAllLogs() {
        return ResponseEntity.ok(
                ApiResponse.<List<AuditLogResponse>>builder()
                        .success(true)
                        .message("Audit logs fetched successfully")
                        .data(auditLogService.getAllLogs())
                        .build()
        );
    }
}