package com.dapfintech.audit.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeActivityItemResponse {

    private UUID id;

    private String action;

    private String title;

    private String description;

    private String moduleName;

    private String entityId;

    private LocalDateTime actionTime;
}