package com.dapfintech.audit.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogResponse {

    private UUID id;

    private String userName;

    private String action;

    private String moduleName;

    private String entityId;

    private LocalDateTime actionTime;
}