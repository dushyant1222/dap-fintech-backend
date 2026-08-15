package com.dapfintech.auth.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.audit.constants.AuditActions;
import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.auth.dto.request.UpdateEmployeePermissionItemRequest;
import com.dapfintech.auth.dto.request.UpdateEmployeePermissionsRequest;
import com.dapfintech.auth.dto.response.EmployeePermissionItemResponse;
import com.dapfintech.auth.dto.response.EmployeePermissionsResponse;
import com.dapfintech.auth.entity.EmployeePermission;
import com.dapfintech.auth.entity.Permission;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.mapper.EmployeePermissionMapper;
import com.dapfintech.auth.repository.EmployeePermissionRepository;
import com.dapfintech.auth.repository.PermissionRepository;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.auth.service.EmployeePermissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeePermissionServiceImpl
        implements EmployeePermissionService {

    private final UserRepository userRepository;

    private final PermissionRepository permissionRepository;

    private final EmployeePermissionRepository
            employeePermissionRepository;

    private final EmployeePermissionMapper
            employeePermissionMapper;
    
    private final AuditLogService auditLogService;

    // =====================================================
    // GET ALL PERMISSIONS FOR ONE EMPLOYEE
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public EmployeePermissionsResponse
    getEmployeePermissions(
            UUID employeeId
    ) {

        User employee = getEmployee(
                employeeId
        );

        List<Permission> allPermissions =
                permissionRepository
                        .findAllByOrderByModuleNameAscPermissionKeyAsc();

        List<EmployeePermission> assignedPermissions =
                employeePermissionRepository
                        .findByEmployeeId(
                                employeeId
                        );

        Map<UUID, Boolean> allowedMap =
                new HashMap<>();

        for (EmployeePermission assigned
                : assignedPermissions) {

            allowedMap.put(
                    assigned.getPermission().getId(),
                    Boolean.TRUE.equals(
                            assigned.getAllowed()
                    )
            );
        }

        List<EmployeePermissionItemResponse>
                permissionResponses =
                allPermissions
                        .stream()
                        .map(permission ->

                                employeePermissionMapper
                                        .toResponse(
                                                permission,
                                                allowedMap
                                                        .getOrDefault(
                                                                permission.getId(),
                                                                false
                                                        )
                                        )
                        )
                        .toList();

        return EmployeePermissionsResponse
                .builder()

                .employeeId(
                        employee.getId()
                )

                .employeeName(
                        employee.getFullName()
                )

                .permissions(
                        permissionResponses
                )

                .build();
    }

    // =====================================================
    // UPDATE ALL PERMISSIONS FOR ONE EMPLOYEE
    // =====================================================

    @Override
    @Transactional
    public EmployeePermissionsResponse
    updateEmployeePermissions(
            UUID employeeId,
            UpdateEmployeePermissionsRequest request
    ) {

        User employee = getEmployee(
                employeeId
        );

        if (request == null ||
                request.getPermissions() == null) {

            throw new RuntimeException(
                    "Permissions list is required"
            );
        }

        List<Permission> allPermissions = permissionRepository.findAll();
        Map<UUID, Permission> permissionMap = new HashMap<>();
        for (Permission p : allPermissions) {
            permissionMap.put(p.getId(), p);
        }

        List<EmployeePermission> existingPermissions = employeePermissionRepository.findByEmployeeId(employeeId);
        Map<UUID, EmployeePermission> existingMap = new HashMap<>();
        for (EmployeePermission ep : existingPermissions) {
            existingMap.put(ep.getPermission().getId(), ep);
        }

        List<EmployeePermission> toSave = new java.util.ArrayList<>();

        for (UpdateEmployeePermissionItemRequest item : request.getPermissions()) {

            if (item.getPermissionId() == null) {
                throw new RuntimeException("Permission ID is required");
            }

            Permission permission = permissionMap.get(item.getPermissionId());
            if (permission == null) {
                throw new RuntimeException("Permission not found: " + item.getPermissionId());
            }

            EmployeePermission employeePermission = existingMap.get(permission.getId());
            if (employeePermission == null) {
                employeePermission = EmployeePermission.builder()
                        .employee(employee)
                        .permission(permission)
                        .allowed(false)
                        .build();
            }

            employeePermission.setAllowed(Boolean.TRUE.equals(item.getAllowed()));
            toSave.add(employeePermission);
        }
        
        employeePermissionRepository.saveAll(toSave);

        auditLogService.logEmployeeActivity(
                employee.getId(),
                employee.getFullName(),
                AuditActions.PERMISSIONS_UPDATED,
                "EMPLOYEE_PERMISSION",
                employee.getId().toString()
        );

        return getEmployeePermissions(
                employeeId
        );
    }

    // =====================================================
    // CHECK EMPLOYEE PERMISSION
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(
            UUID employeeId,
            String permissionKey
    ) {

        if (employeeId == null ||
                permissionKey == null ||
                permissionKey.isBlank()) {

            return false;
        }

        return employeePermissionRepository
                .existsByEmployeeIdAndPermissionPermissionKeyAndAllowedTrue(
                        employeeId,
                        permissionKey
                );
    }

    // =====================================================
    // GET AND VALIDATE EMPLOYEE
    // =====================================================

    private User getEmployee(
            UUID employeeId
    ) {

        User employee =
                userRepository
                        .findById(
                                employeeId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found"
                                )
                        );

        if (employee.getRole() == null ||
                employee.getRole().getRoleName() == null ||
                !employee.getRole()
                        .getRoleName()
                        .equalsIgnoreCase(
                                "EMPLOYEE"
                        )) {

            throw new RuntimeException(
                    "Selected user is not an employee"
            );
        }

        return employee;
    }
}