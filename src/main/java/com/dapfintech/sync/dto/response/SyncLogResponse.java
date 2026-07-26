package com.dapfintech.sync.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.sync.enums.SyncStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyncLogResponse {

    private UUID id;

    private String entityType;

    private String entityId;

    private SyncStatus syncStatus;

    private LocalDateTime syncTime;
}