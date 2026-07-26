package com.dapfintech.auth.service;

import java.util.UUID;

import com.dapfintech.auth.dto.request.UpdateEmployeePermissionsRequest;
import com.dapfintech.auth.dto.response.EmployeePermissionsResponse;

public interface EmployeePermissionService {

    EmployeePermissionsResponse getEmployeePermissions(
            UUID employeeId
    );

    EmployeePermissionsResponse updateEmployeePermissions(
            UUID employeeId,
            UpdateEmployeePermissionsRequest request
    );

    boolean hasPermission(
            UUID employeeId,
            String permissionKey
    );
}