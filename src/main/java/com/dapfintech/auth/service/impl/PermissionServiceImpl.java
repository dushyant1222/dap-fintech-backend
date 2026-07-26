package com.dapfintech.auth.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.auth.dto.response.PermissionResponse;
import com.dapfintech.auth.entity.Permission;
import com.dapfintech.auth.repository.PermissionRepository;
import com.dapfintech.auth.service.PermissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl
        implements PermissionService {

    private final PermissionRepository
            permissionRepository;

    // =====================================================
    // GET ALL AVAILABLE PERMISSIONS
    // =====================================================

    @Override
    public List<PermissionResponse>
    getAllPermissions() {

        return permissionRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // MAP ENTITY TO RESPONSE
    // =====================================================

    private PermissionResponse toResponse(
            Permission permission
    ) {

        return PermissionResponse
                .builder()

                .id(
                        permission.getId()
                )

                .permissionKey(
                        permission
                                .getPermissionKey()
                )

                .moduleName(
                        permission
                                .getModuleName()
                )

                .description(
                        permission
                                .getDescription()
                )

                .build();
    }
}