package com.dapfintech.audit.service;

import java.util.List;
import java.util.UUID;

import com.dapfintech.audit.dto.response.AuditLogResponse;

public interface AuditLogService {
	
    void log(String userName,String action,String moduleName,String entityId);
    void logEmployeeActivity(UUID userId,String userName,String action,String moduleName,String entityId);
    List<AuditLogResponse> getAllLogs();
}