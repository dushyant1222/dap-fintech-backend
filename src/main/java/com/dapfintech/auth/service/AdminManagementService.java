package com.dapfintech.auth.service;

import java.util.List;
import java.util.UUID;
import com.dapfintech.auth.dto.request.CreateAdminRequest;
import com.dapfintech.auth.dto.response.AdminResponse;

public interface AdminManagementService {

    AdminResponse createAdmin(CreateAdminRequest request);

    List<AdminResponse> getAllAdmins();

    void toggleAdminStatus(UUID adminId);
}
