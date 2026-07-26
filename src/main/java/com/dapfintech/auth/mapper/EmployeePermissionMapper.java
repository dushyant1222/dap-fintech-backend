package com.dapfintech.auth.mapper;

import org.springframework.stereotype.Component;

import com.dapfintech.auth.dto.response.EmployeePermissionItemResponse;
import com.dapfintech.auth.entity.EmployeePermission;
import com.dapfintech.auth.entity.Permission;

@Component
public class EmployeePermissionMapper {

    public EmployeePermissionItemResponse toResponse(
            Permission permission,
            boolean allowed
    ) {

        return EmployeePermissionItemResponse
                .builder()

                .permissionId(
                        permission.getId()
                )

                .permissionKey(
                        permission.getPermissionKey()
                )

                .moduleName(
                        permission.getModuleName()
                )

                .description(
                        permission.getDescription()
                )

                .allowed(
                        allowed
                )

                .build();
    }

    public EmployeePermissionItemResponse toResponse(
            EmployeePermission employeePermission
    ) {

        return toResponse(
                employeePermission.getPermission(),
                Boolean.TRUE.equals(
                        employeePermission.getAllowed()
                )
        );
    }
}