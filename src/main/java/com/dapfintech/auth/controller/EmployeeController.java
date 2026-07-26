package com.dapfintech.auth.controller;
import com.dapfintech.auth.dto.request.EmployeeFilterRequest;
import com.dapfintech.auth.dto.response.EmployeePageResponse;
import com.dapfintech.common.response.ApiResponse;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dapfintech.auth.dto.request.CreateEmployeeRequest;
import com.dapfintech.auth.dto.request.UpdateEmployeeRequest;
import com.dapfintech.auth.dto.response.EmployeeResponse;
import com.dapfintech.auth.dto.response.MyProfileResponse;
import com.dapfintech.auth.service.EmployeeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@RequestBody CreateEmployeeRequest request) {

        return ResponseEntity.ok(employeeService.createEmployee(request));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable UUID employeeId,@RequestBody UpdateEmployeeRequest request) {

        return ResponseEntity.ok(employeeService.updateEmployee(employeeId,request));
    }
    
    @GetMapping("/me")
    public ResponseEntity<MyProfileResponse> getMyProfile() {

        return ResponseEntity.ok(employeeService.getMyProfile());

    }
    @PostMapping("/filter")
    public ResponseEntity<ApiResponse<EmployeePageResponse>> filterEmployees(@RequestBody EmployeeFilterRequest request, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        EmployeePageResponse response = employeeService.filterEmployees(request,page,size);

        return ResponseEntity.ok(ApiResponse.<EmployeePageResponse>builder()
                        .success(true)
                        .message("Employees fetched successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable UUID employeeId) {

        return ResponseEntity.ok(
                employeeService.getEmployeeById(employeeId)
        );
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {

        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @PutMapping("/{employeeId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateEmployee(@PathVariable UUID employeeId ) {

        employeeService.activateEmployee(employeeId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Employee activated successfully")
                        .data(null)
                        .build()
        );
    }

    @PutMapping("/{employeeId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(@PathVariable UUID employeeId) {

        employeeService.deactivateEmployee(employeeId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Employee deactivated successfully")
                        .data(null)
                        .build()

        );
    }
}