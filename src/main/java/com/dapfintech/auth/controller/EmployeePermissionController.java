package com.dapfintech.auth.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dapfintech.auth.dto.request.UpdateEmployeePermissionsRequest;
import com.dapfintech.auth.dto.response.EmployeePermissionsResponse;
import com.dapfintech.auth.service.EmployeePermissionService;
import com.dapfintech.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(
        "/api/v1/employees/{employeeId}/permissions"
)
@RequiredArgsConstructor
public class EmployeePermissionController {

    private final EmployeePermissionService employeePermissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<EmployeePermissionsResponse>> getEmployeePermissions(@PathVariable UUID employeeId) {

        EmployeePermissionsResponse response = employeePermissionService.getEmployeePermissions(employeeId);
        return ResponseEntity.ok(
                ApiResponse.<EmployeePermissionsResponse> builder()
                        .success(true)
                        .message("Employee permissions fetched successfully")
                        .data(response)
                        .build()
        );
    }

    @PutMapping
    public ResponseEntity<ApiResponse<EmployeePermissionsResponse>> updateEmployeePermissions(@PathVariable UUID employeeId, @RequestBody UpdateEmployeePermissionsRequest request) {

        EmployeePermissionsResponse response = employeePermissionService.updateEmployeePermissions(employeeId,request);

        return ResponseEntity.ok(ApiResponse.<EmployeePermissionsResponse> builder()
                        .success(true)
                        .message("Employee permissions updated successfully")
                        .data(response)
                        .build()
        );
    }
}