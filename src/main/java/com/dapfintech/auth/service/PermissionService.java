package com.dapfintech.auth.service;

import java.util.List;

import com.dapfintech.auth.dto.response.PermissionResponse;

public interface PermissionService {

    List<PermissionResponse> getAllPermissions();
}