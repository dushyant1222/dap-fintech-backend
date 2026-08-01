package com.dapfintech.auth.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.auth.dto.request.CreateEmployeeRequest;
import com.dapfintech.auth.dto.request.EmployeeFilterRequest;
import com.dapfintech.auth.dto.request.UpdateEmployeeRequest;
import com.dapfintech.auth.dto.response.EmployeePageResponse;
import com.dapfintech.auth.dto.response.EmployeeResponse;
import com.dapfintech.auth.dto.response.MyProfileResponse;

public interface EmployeeService {

    EmployeeResponse createEmployee(
            CreateEmployeeRequest request
    );

    EmployeeResponse updateEmployee(
            UUID employeeId,
            UpdateEmployeeRequest request
    );

    EmployeeResponse getEmployeeById(
            UUID employeeId
    );

    MyProfileResponse getMyProfile();

    List<EmployeeResponse> getAllEmployees();

    EmployeePageResponse filterEmployees(
            EmployeeFilterRequest request,
            int page,
            int size
    );

    void activateEmployee(
            UUID employeeId
    );

    void deactivateEmployee(
            UUID employeeId
    );

    void deleteEmployee(
            UUID employeeId
    );
}